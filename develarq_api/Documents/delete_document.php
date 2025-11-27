<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php'; 

// Validación de token
$headers = getallheaders();
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$database = new Database();
$db = $database->getConnection();

// ✅ Obtener usuario del token
$authHeader = $headers['Authorization'];
$token = str_replace('Bearer ', '', $authHeader);
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

$user_id = $usuario['id'];

try {
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    
    // ✅ Obtener info antes de eliminar
    $infoQuery = "SELECT d.nombre, p.nombre as proyecto_nombre 
                  FROM documentos d 
                  LEFT JOIN proyectos p ON d.proyecto_id = p.id 
                  WHERE d.id = :id";
    $infoStmt = $db->prepare($infoQuery);
    $infoStmt->execute([':id' => $id]);
    $docInfo = $infoStmt->fetch(PDO::FETCH_ASSOC);
    
    // Soft Delete
    $query = "UPDATE documentos SET eliminado = 1, fecha_eliminacion = NOW() WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    
    if ($stmt->execute()) {
        // ✅ REGISTRAR EN AUDITORÍA
        $accion = "Eliminó (movió a papelera) el documento '{$docInfo['nombre']}' del proyecto '{$docInfo['proyecto_nombre']}'";
        registrarAuditoria($db, $user_id, $accion, 'documentos', $id);
        
        http_response_code(200);
        echo json_encode(["success" => true, "message" => "Enviado a papelera"]);
    } else {
        http_response_code(500);
        echo json_encode(["success" => false, "message" => "Error SQL"]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>