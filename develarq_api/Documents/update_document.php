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
    
    // Obtener datos JSON
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    $nombre = isset($data->nombre) ? trim($data->nombre) : '';
    $descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
    $proyecto_id = isset($data->proyecto_id) ? intval($data->proyecto_id) : null;
    $enlace_externo = isset($data->enlace_externo) ? trim($data->enlace_externo) : null;
    
    // Construir query de actualización
    $updates = [];
    $params = [":id" => $id];
    
    if (!empty($nombre)) {
        $updates[] = "nombre = :nombre";
        $params[":nombre"] = $nombre;
    }
    
    if ($descripcion !== null) {
        $updates[] = "descripcion = :descripcion";
        $params[":descripcion"] = $descripcion;
    }
    
    if ($proyecto_id) {
        $updates[] = "proyecto_id = :proyecto_id";
        $params[":proyecto_id"] = $proyecto_id;
    }
    
    if ($enlace_externo !== null) {
        $updates[] = "enlace_externo = :enlace_externo";
        $params[":enlace_externo"] = $enlace_externo;
    }
    
    if (empty($updates)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "No hay datos para actualizar"]);
        exit();
    }
    
    $query = "UPDATE documentos SET " . implode(", ", $updates) . " WHERE id = :id AND eliminado = 0";
    
    $stmt = $db->prepare($query);
    
    foreach ($params as $key => $value) {
        $stmt->bindValue($key, $value);
    }
    
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "Documento actualizado exitosamente"
        ]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error al actualizar documento"]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>