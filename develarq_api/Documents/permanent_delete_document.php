<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Manejo de CORS Pre-flight
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

// Verificación de token
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

try {
    $data = json_decode(file_get_contents("php://input"));
    
    if (!isset($data->id)) {
        http_response_code(400);
        echo json_encode(["success" => false, "message" => "ID de documento requerido"]);
        exit();
    }
    
    $id = intval($data->id);
    
    // Verificar que el documento existe y está eliminado (soft delete)
    $checkQuery = "SELECT id, archivo_url, tipo FROM documentos WHERE id = :id AND eliminado = 1";
    $checkStmt = $db->prepare($checkQuery);
    $checkStmt->bindParam(":id", $id);
    $checkStmt->execute();
    
    if ($checkStmt->rowCount() == 0) {
        http_response_code(404);
        echo json_encode([
            "success" => false,
            "message" => "Documento no encontrado o no está en la papelera"
        ]);
        exit();
    }
    
    $document = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    // Eliminar archivo físico del servidor (solo si no es URL)
    if ($document['tipo'] !== 'URL' && !empty($document['archivo_url'])) {
        $filePath = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/' . $document['archivo_url'];
        
        if (file_exists($filePath)) {
            if (!unlink($filePath)) {
                // No detener el proceso si falla la eliminación del archivo
                error_log("No se pudo eliminar el archivo físico: " . $filePath);
            }
        }
    }
    
    // Eliminación permanente de la base de datos
    $deleteQuery = "DELETE FROM documentos WHERE id = :id";
    $deleteStmt = $db->prepare($deleteQuery);
    $deleteStmt->bindParam(":id", $id);
    
    if ($deleteStmt->execute()) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "Documento eliminado permanentemente"
        ]);
    } else {
        http_response_code(500);
        echo json_encode([
            "success" => false,
            "message" => "Error al eliminar documento de la base de datos"
        ]);
    }
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>