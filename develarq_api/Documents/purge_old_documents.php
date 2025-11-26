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
    // Obtener documentos con más de 30 días en la papelera
    $selectQuery = "SELECT id, archivo_url, tipo, nombre 
                    FROM documentos 
                    WHERE eliminado = 1 
                    AND fecha_eliminacion IS NOT NULL 
                    AND DATEDIFF(NOW(), fecha_eliminacion) >= 30";
    
    $selectStmt = $db->prepare($selectQuery);
    $selectStmt->execute();
    
    $oldDocuments = $selectStmt->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($oldDocuments)) {
        http_response_code(200);
        echo json_encode([
            "success" => true,
            "message" => "No hay documentos con más de 30 días para purgar",
            "deleted_count" => 0
        ]);
        exit();
    }
    
    $deletedCount = 0;
    $errors = [];
    
    // Eliminar cada documento
    foreach ($oldDocuments as $document) {
        try {
            // Eliminar archivo físico (si no es URL)
            if ($document['tipo'] !== 'URL' && !empty($document['archivo_url'])) {
                $filePath = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/' . $document['archivo_url'];
                
                if (file_exists($filePath)) {
                    if (!unlink($filePath)) {
                        error_log("No se pudo eliminar el archivo: " . $filePath);
                    }
                }
            }
            
            // Eliminar de la base de datos
            $deleteQuery = "DELETE FROM documentos WHERE id = :id";
            $deleteStmt = $db->prepare($deleteQuery);
            $deleteStmt->bindParam(":id", $document['id']);
            
            if ($deleteStmt->execute()) {
                $deletedCount++;
            } else {
                $errors[] = "Error al eliminar: " . $document['nombre'];
            }
            
        } catch (Exception $e) {
            $errors[] = "Error con documento ID " . $document['id'] . ": " . $e->getMessage();
        }
    }
    
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "message" => "Purgado completado",
        "deleted_count" => $deletedCount,
        "total_found" => count($oldDocuments),
        "errors" => $errors
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error en la purga: " . $e->getMessage()
    ]);
}
?>