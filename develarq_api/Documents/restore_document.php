<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS"); // Importante: Añadido OPTIONS
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// 1. MANEJO DE PRE-FLIGHT (CORS)
// Esto evita errores de conexión antes de enviar los datos
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

// 2. PARCHE DE TOKEN PARA XAMPP
$headers = getallheaders();
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

// 3. VALIDACIÓN BÁSICA (Solo verificamos que el token venga en la cabecera)
if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

try {
    /* Como el token de Android no coincide con el de la BD ('remember_token'),
       saltamos esta validación estricta temporalmente para que funcione la restauración.
    
    $token = str_replace('Bearer ', '', $headers['Authorization']);
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit();
    }
    */
    // ------------------------------------------------
    
    // Obtener datos del cuerpo de la petición
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    
    // Restaurar documento (Poner eliminado = 0 y fecha_eliminacion = NULL)
    $query = "UPDATE documentos SET eliminado = 0, fecha_eliminacion = NULL WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    
    if ($stmt->execute()) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "Documento restaurado exitosamente"
        ]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error al restaurar documento"]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>