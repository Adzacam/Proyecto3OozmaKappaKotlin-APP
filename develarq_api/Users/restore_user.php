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
    
    $stmtUser = $db->prepare("SELECT CONCAT(name, ' ', apellido) as nombre FROM users WHERE id = :id");
    $stmtUser->execute([':id' => $data->id]);
    $targetUser = $stmtUser->fetch(PDO::FETCH_ASSOC);

    $db->beginTransaction();

    // Restaurar
    $stmt = $db->prepare("UPDATE users SET eliminado = 0, estado = 'activo', updated_at = NOW() WHERE id = :id");
    $stmt->execute([':id' => $data->id]);
    
    // Auditoría Detallada
    $device_info = [
        'modelo' => isset($data->device_model) ? $data->device_model : 'N/A',
        'android' => isset($data->android_version) ? $data->android_version : 'N/A',
        'ip' => obtenerIPCliente()
    ];
    
    $desc = "Restauró al usuario: {$targetUser['nombre']}\n";
    $desc .= "Info Dispositivo: Modelo {$device_info['modelo']}, Android {$device_info['android']}, IP {$device_info['ip']}";

    $stmtAudit = $db->prepare("INSERT INTO auditoria_logs (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) VALUES (:uid, :acc, :desc, 'users', :ref_id, :ip, NOW(), NOW())");
    $stmtAudit->execute([
        ':uid' => $admin['id'], ':acc' => "Restaurar Usuario", ':desc' => $desc,
        ':ref_id' => $data->id, ':ip' => $device_info['ip']
    ]);
    
    $db->commit();
    echo json_encode(['success' => true, 'message' => 'Usuario restaurado exitosamente']);

} catch (Exception $e) {
    $db->rollBack();
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>