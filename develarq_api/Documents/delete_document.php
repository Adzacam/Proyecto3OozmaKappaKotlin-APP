<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';

$headers = apache_request_headers();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

try {
    // Verificar token
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit();
    }
    
    // Obtener datos
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    
    // Marcar como eliminado (soft delete)
    $query = "UPDATE documentos SET eliminado = 1, fecha_eliminacion = NOW() WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "Documento enviado a la papelera"
        ]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error al eliminar documento"]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>