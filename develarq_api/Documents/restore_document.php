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
require_once '../db_config/audit_helper.php'; 

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
$authHeader = $headers['Authorization'];
$token = str_replace('Bearer ', '', $authHeader);
$usuario = obtenerUsuarioDesdeToken($db, $token);

$database = new Database();
$db = $database->getConnection();

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
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    
    // ✅ Obtener info antes de restaurar
    $infoQuery = "SELECT d.nombre, p.nombre as proyecto_nombre 
                  FROM documentos d 
                  LEFT JOIN proyectos p ON d.proyecto_id = p.id 
                  WHERE d.id = :id";
    $infoStmt = $db->prepare($infoQuery);
    $infoStmt->execute([':id' => $id]);
    $docInfo = $infoStmt->fetch(PDO::FETCH_ASSOC);
    
    // Restaurar documento
    $query = "UPDATE documentos SET eliminado = 0, fecha_eliminacion = NULL WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    
    if ($stmt->execute()) {
        // ✅ REGISTRAR EN AUDITORÍA
        $accion = "Restauró el documento '{$docInfo['nombre']}' del proyecto '{$docInfo['proyecto_nombre']}'";
        registrarAuditoria($db, $user_id, $accion, 'documentos', $id);
        
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