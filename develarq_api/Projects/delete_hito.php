<?php
// ============================================
// ELIMINAR HITO
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, DELETE");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;
$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token inválido"]); exit(); }

// Validar permisos (Solo Admin)
if (strtolower($usuario['rol']) !== 'admin') {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos"]);
    exit();
}

$data = json_decode(file_get_contents("php://input"));
$id = isset($data->id) ? intval($data->id) : null;
$device_info = extraerInfoDispositivo($data);

if (!$id) { http_response_code(400); echo json_encode(["success" => false, "message" => "ID requerido"]); exit(); }

try {
    // 1. Obtener datos del hito ANTES de borrar para la notificación
    $query_info = "SELECT h.nombre, h.proyecto_id, p.nombre as proyecto_nombre 
                   FROM hitos h 
                   JOIN proyectos p ON h.proyecto_id = p.id 
                   WHERE h.id = :id";
    $stmt_info = $db->prepare($query_info);
    $stmt_info->execute([':id' => $id]);
    $hito = $stmt_info->fetch(PDO::FETCH_ASSOC);

    if (!$hito) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Hito no encontrado"]);
        exit();
    }

    // 2. Eliminar
    $query = "DELETE FROM hitos WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->execute([':id' => $id]);

    // 3. Auditoría
    registrarAuditoriaCompleta($db, $usuario['id'], "Eliminó el hito '{$hito['nombre']}'", 'hitos', $id, $device_info);

    // 4. NOTIFICACIONES (Estilo BIM)
    try {
        $query_usuarios = "SELECT DISTINCT user_id FROM proyectos_usuarios WHERE proyecto_id = :proyecto_id AND eliminado = 0";
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':proyecto_id' => $hito['proyecto_id']]);

        $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at) VALUES (:user_id, :mensaje, 'hito', :asunto, :url, NOW())";
        $stmt_notif = $db->prepare($query_notif);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El hito '{$hito['nombre']}' del proyecto '{$hito['proyecto_nombre']}' ha sido eliminado por un administrador.",
                ':asunto' => 'Hito eliminado',
                ':url' => "http://127.0.0.1:8000/proyectos/{$hito['proyecto_id']}"
            ]);
        }
    } catch (Exception $e) { error_log("Error notif: " . $e->getMessage()); }

    echo json_encode(["success" => true, "message" => "Hito eliminado"]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error: " . $e->getMessage()]);
}
?>