<?php
header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';

try {
    $headers = getallheaders();
    $token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

    if (!$token) { throw new Exception("Token no proporcionado"); }

    $database = new Database();
    $db = $database->getConnection();

    $stmt = $db->prepare("SELECT id FROM users WHERE remember_token = :token AND eliminado = 0");
    $stmt->execute([':token' => $token]);
    if ($stmt->rowCount() == 0) { throw new Exception("Usuario no autorizado"); }
    $user = $stmt->fetch(PDO::FETCH_ASSOC);

    // Actualizar todas
    $update = $db->prepare("UPDATE notificaciones SET leida = 1 WHERE user_id = :user_id AND leida = 0");
    $update->execute([':user_id' => $user['id']]);

    echo json_encode(["success" => true, "message" => "Todas marcadas como leídas"]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>