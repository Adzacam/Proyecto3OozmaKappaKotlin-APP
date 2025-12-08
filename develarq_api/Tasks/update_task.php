<?php
// ============================================
// API: UPDATE TASK - Actualizar tarea
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, PUT, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// VERIFICAR TOKEN
// ============================================
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';

if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$token = $matches[1];
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// OBTENER DATOS
// ============================================
$data = json_decode(file_get_contents("php://input"));

if (!$data || empty($data->tarea_id)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de tarea requerido"]);
    exit();
}

$tareaId = $data->tarea_id;
$userId = $usuario['id'];
$deviceInfo = extraerInfoDispositivo($data);

try {
    $db->beginTransaction();

    // ============================================
    // OBTENER TAREA ACTUAL
    // ============================================
    $queryTarea = "SELECT t.*, p.nombre as proyecto_nombre
                   FROM tareas t
                   INNER JOIN proyectos p ON t.proyecto_id = p.id
                   WHERE t.id = :tarea_id AND t.eliminado = 0";
    
    $stmtTarea = $db->prepare($queryTarea);
    $stmtTarea->execute([':tarea_id' => $tareaId]);

    if ($stmtTarea->rowCount() === 0) {
        throw new Exception("Tarea no encontrada");
    }

    $tareaActual = $stmtTarea->fetch(PDO::FETCH_ASSOC);

    // ============================================
    // VERIFICAR PERMISOS
    // ============================================
    $queryPermiso = "SELECT 1 FROM proyectos p
                     LEFT JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id AND pu.user_id = :user_id
                     WHERE p.id = :proyecto_id
                     AND (
                         p.responsable_id = :user_id
                         OR p.cliente_id = :user_id
                         OR pu.permiso = 'editar'
                         OR :user_rol = 'admin'
                         OR t.creador_id = :user_id
                     )";

    $stmtPermiso = $db->prepare($queryPermiso);
    $stmtPermiso->execute([
        ':proyecto_id' => $tareaActual['proyecto_id'],
        ':user_id' => $userId,
        ':user_rol' => $usuario['rol']
    ]);

    if ($stmtPermiso->rowCount() === 0) {
        throw new Exception("No tienes permisos para editar esta tarea");
    }

    // ============================================
    // PREPARAR CAMPOS A ACTUALIZAR
    // ============================================
    $camposUpdate = [];
    $params = [':tarea_id' => $tareaId];
    $cambios = [];

    // Título
    if (isset($data->titulo) && $data->titulo !== $tareaActual['titulo']) {
        $camposUpdate[] = "titulo = :titulo";
        $params[':titulo'] = trim($data->titulo);
        $cambios[] = "Título cambiado de '{$tareaActual['titulo']}' a '{$data->titulo}'";
    }

    // Descripción
    if (isset($data->descripcion) && $data->descripcion !== $tareaActual['descripcion']) {
        $camposUpdate[] = "descripcion = :descripcion";
        $params[':descripcion'] = trim($data->descripcion);
        $cambios[] = "Descripción actualizada";
    }

    // Estado
    if (isset($data->estado) && $data->estado !== $tareaActual['estado']) {
        $camposUpdate[] = "estado = :estado";
        $params[':estado'] = $data->estado;
        $cambios[] = "Estado cambiado de '{$tareaActual['estado']}' a '{$data->estado}'";

        // Registrar en historial de cambios de estado
        $queryHistorial = "INSERT INTO tarea_historials (
                              proyecto_id, tarea_id, usuario_id,
                              estado_anterior, estado_nuevo, cambio,
                              fecha_cambio, created_at, updated_at
                           ) VALUES (
                              :proyecto_id, :tarea_id, :usuario_id,
                              :estado_anterior, :estado_nuevo, :cambio,
                              NOW(), NOW(), NOW()
                           )";
        
        $stmtHistorial = $db->prepare($queryHistorial);
        $stmtHistorial->execute([
            ':proyecto_id' => $tareaActual['proyecto_id'],
            ':tarea_id' => $tareaId,
            ':usuario_id' => $userId,
            ':estado_anterior' => $tareaActual['estado'],
            ':estado_nuevo' => $data->estado,
            ':cambio' => "Cambio de estado de '{$tareaActual['estado']}' a '{$data->estado}'"
        ]);
    }

    // Prioridad
    if (isset($data->prioridad) && $data->prioridad !== $tareaActual['prioridad']) {
        $camposUpdate[] = "prioridad = :prioridad";
        $params[':prioridad'] = $data->prioridad;
        $cambios[] = "Prioridad cambiada de '{$tareaActual['prioridad']}' a '{$data->prioridad}'";
    }

    // Fecha límite
    if (isset($data->fecha_limite) && $data->fecha_limite !== $tareaActual['fecha_limite']) {
        $camposUpdate[] = "fecha_limite = :fecha_limite";
        $params[':fecha_limite'] = $data->fecha_limite;
        $cambios[] = "Fecha límite actualizada";
    }

    // Asignado
    if (isset($data->asignado_id) && $data->asignado_id !== $tareaActual['asignado_id']) {
        $camposUpdate[] = "asignado_id = :asignado_id";
        $params[':asignado_id'] = $data->asignado_id;
        $cambios[] = "Usuario asignado actualizado";

        // Notificar al nuevo asignado
        if ($data->asignado_id && $data->asignado_id != $userId) {
            $queryNotif = "INSERT INTO notificaciones (
                              user_id, mensaje, tipo, asunto, url,
                              fecha_envio, created_at, updated_at
                           ) VALUES (
                              :user_id, :mensaje, 'tarea', :asunto, :url,
                              NOW(), NOW(), NOW()
                           )";
            
            $stmtNotif = $db->prepare($queryNotif);
            $stmtNotif->execute([
                ':user_id' => $data->asignado_id,
                ':mensaje' => "Se te ha asignado la tarea '{$tareaActual['titulo']}' en el proyecto '{$tareaActual['proyecto_nombre']}'.",
                ':asunto' => 'Tarea asignada',
                ':url' => "http://127.0.0.1:8000/proyectos/{$tareaActual['proyecto_id']}"
            ]);
        }
    }

    // Si no hay cambios
    if (empty($camposUpdate)) {
        throw new Exception("No se detectaron cambios");
    }

    // ============================================
    // EJECUTAR UPDATE
    // ============================================
    $camposUpdate[] = "updated_at = NOW()";
    $queryUpdate = "UPDATE tareas SET " . implode(", ", $camposUpdate) . " WHERE id = :tarea_id";

    $stmtUpdate = $db->prepare($queryUpdate);
    $stmtUpdate->execute($params);

    // ============================================
    // AUDITORÍA
    // ============================================
    $accion = "Actualizó la tarea '{$tareaActual['titulo']}': " . implode(", ", $cambios);
    
    registrarAuditoriaCompleta(
        $db,
        $userId,
        $accion,
        'tareas',
        $tareaId,
        $deviceInfo
    );

    $db->commit();

    // ============================================
    // RESPUESTA
    // ============================================
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Tarea actualizada exitosamente"
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al actualizar tarea: " . $e->getMessage()
    ]);
}
?>