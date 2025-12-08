<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

$data = json_decode(file_get_contents("php://input"));
if (empty($data->id)) { echo json_encode(['success' => false, 'message' => 'ID requerido']); exit; }

$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';
if (empty($authHeader) || !preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    http_response_code(401); echo json_encode(["success" => false, "message" => "Token faltante"]); exit;
}
$token = $matches[1];

try {
    $database = new Database();
    $db = $database->getConnection();

    $admin = obtenerUsuarioDesdeToken($db, $token);
    if (!$admin) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token inválido"]); exit; }

    // Obtener estado actual
    $stmt = $db->prepare("SELECT estado, name FROM users WHERE id = :id AND eliminado = 0");
    $stmt->execute([':id' => $data->id]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$user) { echo json_encode(['success' => false, 'message' => 'Usuario no encontrado']); exit; }

    $newStatus = ($user['estado'] === 'activo') ? 'inactivo' : 'activo';
    
    $db->beginTransaction();

    $updateStmt = $db->prepare("UPDATE users SET estado = :estado, updated_at = NOW() WHERE id = :id");
    $updateStmt->execute([':estado' => $newStatus, ':id' => $data->id]);
    
    // Auditoría
    $device_info = [
        'modelo' => isset($data->device_model) ? $data->device_model : 'N/A',
        'android' => isset($data->android_version) ? $data->android_version : 'N/A',
        'ip' => obtenerIPCliente()
    ];

    $desc = "Cambió estado de '{$user['name']}' a: $newStatus.\n";
    $desc .= "Info Dispositivo: Modelo {$device_info['modelo']}, Android {$device_info['android']}, IP {$device_info['ip']}";

    $stmtAudit = $db->prepare("INSERT INTO auditoria_logs (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) VALUES (:uid, :acc, :desc, 'users', :ref_id, :ip, NOW(), NOW())");
    $stmtAudit->execute([
        ':uid' => $admin['id'], ':acc' => "Cambiar Estado", ':desc' => $desc,
        ':ref_id' => $data->id, ':ip' => $device_info['ip']
    ]);

    $db->commit();
    echo json_encode(['success' => true, 'message' => 'Estado actualizado', 'data' => ['estado' => $newStatus]]);

} catch (Exception $e) {
    $db->rollBack();
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>