<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

// --- VALIDACIÓN DE TOKEN ---
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
$user_id = 1; // Fallback

try {
    $id = isset($_GET['id']) ? intval($_GET['id']) : 0;
    
    if ($id == 0) { throw new Exception("ID requerido"); }
    
    // Obtener datos del documento
    $query = "SELECT * FROM documentos WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":id", $id);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Documento no encontrado en BD"]);
        exit();
    }
    
    $document = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Si es URL externa, devolver el link
    if ($document['tipo'] == 'URL') {
        echo json_encode(["success" => true, "url" => $document['enlace_externo']]);
        exit();
    }
    
    // --- LÓGICA DE BÚSQUEDA FÍSICA ---
    $filename = basename($document['archivo_url']);
    
    // 1. RUTA PRINCIPAL: La Bóveda de Laravel (storage/app/public)
    $ruta_laravel = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/documentos/' . $filename;
    // 2. RUTA RESPALDO: Carpeta local de XAMPP (para archivos viejos)
    $ruta_xampp = '../storage/documentos/' . $filename;
    
    $file_to_serve = null;
    
    if (file_exists($ruta_laravel)) {
        $file_to_serve = $ruta_laravel;
    } elseif (file_exists($ruta_xampp)) {
        $file_to_serve = $ruta_xampp;
    } else {
        http_response_code(404);
        // Devolvemos JSON para que la app sepa qué pasó, aunque DownloadManager a veces ignora esto.
        echo json_encode([
            "success" => false, 
            "message" => "Archivo físico no encontrado. Buscado en: " . $ruta_laravel
        ]);
        exit();
    }
    
    // --- SERVIR EL ARCHIVO ---
    
    // Limpiar buffer de salida para evitar archivos corruptos
    if (ob_get_level()) ob_end_clean();
    
    header('Content-Description: File Transfer');
    header('Content-Type: application/octet-stream');
    // Forzamos el nombre original para la descarga
    header('Content-Disposition: attachment; filename="' . basename($document['nombre']) . '.' . pathinfo($filename, PATHINFO_EXTENSION) . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($file_to_serve));
    
    readfile($file_to_serve);
    exit();
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(["success" => false, "message" => "Error: " . $e->getMessage()]);
}
?>