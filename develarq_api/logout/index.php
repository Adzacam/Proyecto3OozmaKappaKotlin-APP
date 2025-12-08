<?php
// ==========================================
// logout.php (Con Auditoría)
// ==========================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php'; // Helper de auditoría

// Obtener Token del Header
$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';

// Soporte para servidores que no pasan Authorization header automáticamente
if (empty($authHeader) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
}

if (empty($authHeader)) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$token = str_replace('Bearer ', '', $authHeader);
$data = json_decode(file_get_contents("php://input"));

try {
    $database = new Database();
    $db = $database->getConnection();

    // 1. Identificar usuario antes de borrar el token
    $findQuery = "SELECT id, name, apellido FROM users WHERE remember_token = :token AND eliminado = 0";
    $findStmt = $db->prepare($findQuery);
    $findStmt->bindParam(':token', $token);
    $findStmt->execute();

    if ($findStmt->rowCount() > 0) {
        $user = $findStmt->fetch(PDO::FETCH_ASSOC);

        // 2. Registrar Auditoría
        $device_info = [
            'modelo' => isset($data->device_model) ? $data->device_model : 'Web/Desconocido',
            'android' => isset($data->android_version) ? $data->android_version : 'N/A',
            'ip' => obtenerIPCliente()
        ];

        $descripcion = "Cierre de sesión.\n";
        $descripcion .= "Dispositivo: {$device_info['modelo']}\n";
        $descripcion .= "Android: {$device_info['android']}\n";
        $descripcion .= "IP: {$device_info['ip']}";

        $queryAudit = "INSERT INTO auditoria_logs 
                      (user_id, accion, descripcion_detallada, tabla_afectada, id_registro_afectado, ip_address, fecha_accion, created_at) 
                      VALUES 
                      (:uid, 'Cierre de Sesión', :desc, 'users', :ref_id, :ip, NOW(), NOW())";
        
        $stmtAudit = $db->prepare($queryAudit);
        $stmtAudit->execute([
            ':uid' => $user['id'],
            ':desc' => $descripcion,
            ':ref_id' => $user['id'],
            ':ip' => $device_info['ip']
        ]);

        // 3. Invalidar Token
        $logoutQuery = "UPDATE users SET remember_token = NULL, updated_at = NOW() WHERE id = :id";
        $logoutStmt = $db->prepare($logoutQuery);
        $logoutStmt->bindParam(':id', $user['id']);
        $logoutStmt->execute();

        echo json_encode([
            "success" => true,
            "message" => "Sesión cerrada correctamente",
            "user" => $user['name']
        ]);
    } else {
        // El token no existe o ya expiró, igual respondemos éxito para que la app limpie la sesión local
        echo json_encode([
            "success" => true,
            "message" => "Sesión cerrada (Token inválido o ya expirado)"
        ]);
    }

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error del servidor: " . $e->getMessage()
    ]);
}
?>