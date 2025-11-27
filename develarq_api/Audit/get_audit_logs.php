<?php
// api/Audit/get_audit_logs.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// Verificar token
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

// Verificar que sea admin
$queryRole = "SELECT rol FROM users WHERE id = :user_id LIMIT 1";
$stmtRole = $db->prepare($queryRole);
$stmtRole->execute([':user_id' => $usuario['id']]);
$userRole = $stmtRole->fetch(PDO::FETCH_ASSOC);

if (!$userRole || strtolower($userRole['rol']) !== 'admin') {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Acceso denegado"]);
    exit();
}

try {
    $query = "SELECT 
                a.id,
                CONCAT(u.name, ' ', u.apellido) as usuario,
                a.accion,
                a.id_registro_afectado as registro,
                DATE_FORMAT(a.fecha_accion, '%d/%m/%Y, %H:%i:%s') as fecha,
                a.ip_address
              FROM auditoria_logs a
              INNER JOIN users u ON a.user_id = u.id
              ORDER BY a.fecha_accion DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    $logs = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    http_response_code(200);
    echo json_encode($logs);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener logs: " . $e->getMessage()
    ]);
}
?>