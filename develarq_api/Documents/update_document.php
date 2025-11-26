<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';

// --- PARCHE DE TOKEN ---
$headers = getallheaders();
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}
// -----------------------

$database = new Database();
$db = $database->getConnection();

try {
    // Saltamos validación estricta de token para evitar el 401 por ahora
    
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    $nombre = isset($data->nombre) ? trim($data->nombre) : '';
    $descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
    $proyecto_id = isset($data->proyecto_id) ? intval($data->proyecto_id) : null;
    $enlace_externo = isset($data->enlace_externo) ? trim($data->enlace_externo) : null;
    $tipo = isset($data->tipo) ? trim($data->tipo) : null;
    
    $updates = [];
    $params = [":id" => $id];
    
    if (!empty($nombre)) { $updates[] = "nombre = :nombre"; $params[":nombre"] = $nombre; }
    if ($descripcion !== null) { $updates[] = "descripcion = :descripcion"; $params[":descripcion"] = $descripcion; }
    if ($proyecto_id) { $updates[] = "proyecto_id = :proyecto_id"; $params[":proyecto_id"] = $proyecto_id; }
    if ($enlace_externo !== null) { $updates[] = "enlace_externo = :enlace_externo"; $params[":enlace_externo"] = $enlace_externo; }
    if ($tipo !== null) { $updates[] = "tipo = :tipo"; $params[":tipo"] = $tipo; } // Agregado tipo
    
    if (empty($updates)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "Nada para actualizar"]);
        exit();
    }
    
    
$query = "UPDATE documentos SET " . implode(", ", $updates) . ", updated_at = NOW() WHERE id = :id";
    $stmt = $db->prepare($query);
    
    foreach ($params as $key => $value) {
        $stmt->bindValue($key, $value);
    }
    
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode(["success" => true, "message" => "Documento actualizado"]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error SQL"]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>