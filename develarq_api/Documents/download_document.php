<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");
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
    // Verificar token y obtener user_id
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido"]);
        exit();
    }
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    $user_id = $user['id'];
    
    // Obtener ID del documento
    $id = isset($_GET['id']) ? intval($_GET['id']) : 0;
    
    if ($id == 0) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    // Obtener información del documento
    $query = "SELECT * FROM documentos WHERE id = :id AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Documento no encontrado"]);
        exit();
    }
    
    $document = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Registrar descarga
    $query = "INSERT INTO descargas_historial (user_id, documento_id, created_at) VALUES (:user_id, :documento_id, NOW())";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":user_id", $user_id);
    $stmt->bindParam(":documento_id", $id);
    $stmt->execute();
    
    // Si es URL, devolver la URL
    if ($document['tipo'] == 'URL') {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "url" => $document['enlace_externo'],
            "message" => "URL del documento"
        ]);
        exit();
    }
    
    // Para archivos, preparar descarga
    $file_path = '../../' . $document['archivo_url'];
    
    if (!file_exists($file_path)) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Archivo no encontrado en el servidor"]);
        exit();
    }
    
    // Enviar archivo
    header('Content-Description: File Transfer');
    header('Content-Type: application/octet-stream');
    header('Content-Disposition: attachment; filename="' . basename($document['nombre']) . '"');
    header('Content-Length: ' . filesize($file_path));
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    
    readfile($file_path);
    exit();
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>