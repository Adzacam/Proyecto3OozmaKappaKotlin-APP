<?php
// ============================================
// ACTUALIZAR HITO
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, PUT");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// AUTENTICACIÓN
// ============================================
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// RECIBIR DATOS
// ============================================
$data = json_decode(file_get_contents("php://input"));

$id = isset($data->id) ? intval($data->id) : null;
$nombre = isset($data->nombre) ? trim($data->nombre) : null;
$fecha_hito = isset($data->fecha_hito) ? trim($data->fecha_hito) : null;
$descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
$estado = isset($data->estado) ? trim($data->estado) : null;
$encargado_id = isset($data->encargado_id) ? intval($data->encargado_id) : null;

$device_info = extraerInfoDispositivo($data);

// ============================================
// VALIDACIONES
// ============================================
if (!$id || !$nombre || !$fecha_hito || !$estado) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Faltan datos obligatorios"]);
    exit();
}

// Verificar que el hito existe
$query_check = "SELECT h.nombre, h.encargado_id, p.nombre as proyecto_nombre 
                FROM hitos h
                INNER JOIN proyectos p ON h.proyecto_id = p.id
                WHERE h.id = :id";
$stmt_check = $db->prepare($query_check);
$stmt_check->execute([':id' => $id]);

if ($stmt_check->rowCount() == 0) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Hito no encontrado"]);
    exit();
}

$hito_actual = $stmt_check->fetch(PDO::FETCH_ASSOC);
$encargado_anterior = $hito_actual['encargado_id'];
$proyecto_nombre = $hito_actual['proyecto_nombre'];

// ============================================
// ACTUALIZAR HITO
// ============================================
try {
    $db->beginTransaction();

    $query = "UPDATE hitos SET 
              nombre = :nombre,
              fecha_hito = :fecha_hito,
              descripcion = :descripcion,
              estado = :estado,
              encargado_id = :encargado_id,
              updated_at = NOW()
              WHERE id = :id";
    
    $stmt = $db->prepare($query);
    $stmt->execute([
        ':nombre' => $nombre,
        ':fecha_hito' => $fecha_hito,
        ':descripcion' => $descripcion,
        ':estado' => $estado,
        ':encargado_id' => $encargado_id,
        ':id' => $id
    ]);

    // ============================================
    // REGISTRAR AUDITORÍA
    // ============================================
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Actualizó el hito '$nombre' a estado '$estado'",
        'hitos',
        $id,
        $device_info
    );

    // ============================================
    // NOTIFICACIONES
    // ============================================
    // Si cambió el encargado, notificar al nuevo
    if ($encargado_id && $encargado_id != $encargado_anterior) {
        $query_notif = "INSERT INTO notificaciones (
                        user_id, mensaje, tipo, asunto, url, leida, eliminado,
                        fecha_envio, created_at, updated_at
                        ) VALUES (
                        :user_id, :mensaje, 'hito', 'Hito asignado', 
                        NULL, 0, 0, NOW(), NOW(), NOW()
                        )";
        $stmt_notif = $db->prepare($query_notif);
        $stmt_notif->execute([
            ':user_id' => $encargado_id,
            ':mensaje' => "Se te ha asignado el hito '$nombre' del proyecto '$proyecto_nombre'"
        ]);
    }

    // Si cambió a completado, notificar a todos los del proyecto
    if ($estado == 'Completado') {
        $query_usuarios = "SELECT DISTINCT user_id FROM proyectos_usuarios 
                          WHERE proyecto_id = (SELECT proyecto_id FROM hitos WHERE id = :hito_id)";
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':hito_id' => $id]);

        $notif_query = "INSERT INTO notificaciones (
                        user_id, mensaje, tipo, asunto, url, leida, eliminado,
                        fecha_envio, created_at, updated_at
                        ) VALUES (
                        :user_id, :mensaje, 'hito', 'Hito completado', 
                        NULL, 0, 0, NOW(), NOW(), NOW()
                        )";
        $stmt_notif = $db->prepare($notif_query);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El hito '$nombre' del proyecto '$proyecto_nombre' ha sido completado"
            ]);
        }
    }

    $db->commit();

    echo json_encode([
        "success" => true,
        "message" => "Hito actualizado exitosamente"
    ]);
    
} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al actualizar hito: " . $e->getMessage()
    ]);
}
?>