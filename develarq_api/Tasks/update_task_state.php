<?php
// ============================================
// API: UPDATE TASK STATE - Mover tarea entre columnas (Drag & Drop)
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

if (!$data || empty($data->tarea_id) || empty($data->estado)) {
    http_response_code(400);
    echo json_encode([
        "success" => false, 
        "message" => "Faltan campos obligatorios: tarea_id, estado"
    ]);
    exit();
}

$tareaId = $data->tarea_id;
$nuevoEstado = $data->estado;
$userId = $usuario['id'];
$deviceInfo = extraerInfoDispositivo($data);

// Validar estado
$estadosValidos = ['pendiente', 'en progreso', 'completado'];
if (!in_array(strtolower($nuevoEstado), $estadosValidos)) {
    http_response_code(400);
    echo json_encode([
        "success" => false,
        "message" => "Estado inválido. Debe ser: pendiente, en progreso o completado"
    ]);
    exit();
}

try {
    $db->beginTransaction();

    // ============================================
    // OBTENER TAREA ACTUAL
    // ============================================
    $queryTarea = "SELECT t.*, p.nombre as proyecto_nombre,
                          CONCAT(u.name, ' ', u.apellido) as asignado_nombre
                   FROM tareas t
                   INNER JOIN proyectos p ON t.proyecto_id = p.id
                   LEFT JOIN users u ON t.asignado_id = u.id
                   WHERE t.id = :tarea_id AND t.eliminado = 0";
    
    $stmtTarea = $db->prepare($queryTarea);
    $stmtTarea->execute([':tarea_id' => $tareaId]);

    if ($stmtTarea->rowCount() === 0) {
        throw new Exception("Tarea no encontrada");
    }

    $tarea = $stmtTarea->fetch(PDO::FETCH_ASSOC);
    $estadoAnterior = $tarea['estado'];

    // Si el estado es el mismo, no hacer nada
    if (strtolower($estadoAnterior) === strtolower($nuevoEstado)) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "La tarea ya está en este estado"
        ]);
        exit();
    }

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
                         OR :tarea_asignado_id = :user_id
                         OR :tarea_creador_id = :user_id
                     )";

    $stmtPermiso = $db->prepare($queryPermiso);
    $stmtPermiso->execute([
        ':proyecto_id' => $tarea['proyecto_id'],
        ':user_id' => $userId,
        ':user_rol' => $usuario['rol'],
        ':tarea_asignado_id' => $tarea['asignado_id'],
        ':tarea_creador_id' => $tarea['creador_id']
    ]);

    if ($stmtPermiso->rowCount() === 0) {
        throw new Exception("No tienes permisos para mover esta tarea");
    }

    // ============================================
    // ACTUALIZAR ESTADO
    // ============================================
    $queryUpdate = "UPDATE tareas 
                    SET estado = :estado, updated_at = NOW() 
                    WHERE id = :tarea_id";

    $stmtUpdate = $db->prepare($queryUpdate);
    $stmtUpdate->execute([
        ':estado' => $nuevoEstado,
        ':tarea_id' => $tareaId
    ]);

    // ============================================
    // REGISTRAR EN HISTORIAL
    // ============================================
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
        ':proyecto_id' => $tarea['proyecto_id'],
        ':tarea_id' => $tareaId,
        ':usuario_id' => $userId,
        ':estado_anterior' => $estadoAnterior,
        ':estado_nuevo' => $nuevoEstado,
        ':cambio' => "Movió la tarea de '{$estadoAnterior}' a '{$nuevoEstado}'"
    ]);

    // ============================================
    // AUDITORÍA
    // ============================================
    $accion = "Movió la tarea '{$tarea['titulo']}' de '{$estadoAnterior}' a '{$nuevoEstado}' en el proyecto '{$tarea['proyecto_nombre']}'.";
    
    registrarAuditoriaCompleta(
        $db,
        $userId,
        $accion,
        'tareas',
        $tareaId,
        $deviceInfo
    );

    // ============================================
    // NOTIFICACIONES
    // ============================================
    // Notificar al asignado si no es el mismo usuario que movió
    if ($tarea['asignado_id'] && $tarea['asignado_id'] != $userId) {
        $queryNotif = "INSERT INTO notificaciones (
                          user_id, mensaje, tipo, asunto, url,
                          fecha_envio, created_at, updated_at
                       ) VALUES (
                          :user_id, :mensaje, 'tarea', :asunto, :url,
                          NOW(), NOW(), NOW()
                       )";
        
        $stmtNotif = $db->prepare($queryNotif);
        $stmtNotif->execute([
            ':user_id' => $tarea['asignado_id'],
            ':mensaje' => "La tarea '{$tarea['titulo']}' fue movida a '{$nuevoEstado}' por {$usuario['nombre_completo']}.",
            ':asunto' => 'Tarea actualizada',
            ':url' => "http://127.0.0.1:8000/proyectos/{$tarea['proyecto_id']}"
        ]);
    }

    // Si la tarea se completó, notificar al creador
    if (strtolower($nuevoEstado) === 'completado' && $tarea['creador_id'] && $tarea['creador_id'] != $userId) {
        $stmtNotif->execute([
            ':user_id' => $tarea['creador_id'],
            ':mensaje' => "La tarea '{$tarea['titulo']}' ha sido completada.",
            ':asunto' => 'Tarea completada',
            ':url' => "http://127.0.0.1:8000/proyectos/{$tarea['proyecto_id']}"
        ]);
    }

    $db->commit();

    // ============================================
    // RESPUESTA
    // ============================================
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Tarea movida exitosamente a '{$nuevoEstado}'",
        "data" => [
            "tarea_id" => $tareaId,
            "estado_anterior" => $estadoAnterior,
            "estado_nuevo" => $nuevoEstado
        ]
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al mover tarea: " . $e->getMessage()
    ]);
}
?>