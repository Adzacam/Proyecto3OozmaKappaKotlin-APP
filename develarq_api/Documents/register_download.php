<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php'; // ✅ NUEVO

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

$authHeader = $headers['Authorization'];
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
} else {
    $token = $authHeader;
}

$database = new Database();
$db = $database->getConnection();

// Obtener user_id y datos del usuario
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

$user_id = $usuario['id'];
$nombre_usuario = $usuario['nombre_completo'];

// Procesar registro de descarga
try {
    $data = json_decode(file_get_contents("php://input"));

    if (!isset($data->documento_id)) {
        throw new Exception("documento_id es requerido");
    }

    $documento_id = $data->documento_id;

    // Obtener información del documento y proyecto
    $docQuery = "SELECT d.nombre, d.proyecto_id, p.nombre as proyecto_nombre 
                 FROM documentos d 
                 LEFT JOIN proyectos p ON d.proyecto_id = p.id 
                 WHERE d.id = :doc_id AND d.eliminado = 0";
    $docStmt = $db->prepare($docQuery);
    $docStmt->execute([':doc_id' => $documento_id]);
    
    if ($docStmt->rowCount() == 0) {
        throw new Exception("Documento no encontrado");
    }
    
    $documento = $docStmt->fetch(PDO::FETCH_ASSOC);

    // Registrar la descarga en descargas_historial
    $insertQuery = "INSERT INTO descargas_historial (user_id, documento_id, created_at, updated_at) 
                    VALUES (:user_id, :documento_id, NOW(), NOW())";
    
    $insertStmt = $db->prepare($insertQuery);
    $insertStmt->execute([
        ':user_id' => $user_id,
        ':documento_id' => $documento_id
    ]);

    // ✅ REGISTRAR EN AUDITORÍA
    $accion = "Descargó el documento '{$documento['nombre']}' del proyecto '{$documento['proyecto_nombre']}'";
    registrarAuditoria($db, $user_id, $accion, 'documentos', $documento_id);

    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Descarga registrada correctamente"
    ]);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>