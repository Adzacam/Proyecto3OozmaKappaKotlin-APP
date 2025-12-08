<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';

// Auth básica (opcional para descarga directa, pero recomendado)
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

$database = new Database();
$db = $database->getConnection();

try {
    $id = isset($_GET['id']) ? intval($_GET['id']) : 0;
    if ($id == 0) throw new Exception("ID requerido");

    // Obtener info del plano
    $query = "SELECT * FROM planos_bim WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->execute([':id' => $id]);
    $plano = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$plano) {
        http_response_code(404);
        die("Plano no encontrado");
    }

    // --- LÓGICA DE RUTAS (Igual que Documents) ---
    // La URL en BD suele ser relativa: "bim_planos/archivo.ifc" o "/storage/planos/..."
    $archivo_db = $plano['archivo_url'];
    $filename = basename($archivo_db);

    // Construir ruta física
    $ruta_relativa = str_replace('/storage/', '', $archivo_db); // "planos/proyecto_1/plano_123.glb"
    
    $base_laravel = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/';
    $ruta_fisica = $base_laravel . $ruta_relativa;

    if (!file_exists($ruta_fisica)) {
        // Intento 2: Buscar solo por nombre en la carpeta genérica (por si acaso)
        $ruta_fisica = $base_laravel . 'bim_planos/' . $filename;
    }

    if (!file_exists($ruta_fisica)) {
        http_response_code(404);
        die("Error: Archivo físico no encontrado en el servidor.\nRuta buscada: $ruta_fisica");
    }

// --- SERVIR ARCHIVO ---
    if (ob_get_level()) ob_end_clean();
    
    // ✅ Determinar Content-Type correcto
    $ext = strtolower(pathinfo($ruta_fisica, PATHINFO_EXTENSION));
    $ctype = "application/octet-stream"; // Default
    
    // Mapeo correcto de tipos MIME
    switch($ext) {
        case "pdf":
            $ctype = "application/pdf";
            break;
        case "jpg":
        case "jpeg":
            $ctype = "image/jpeg";
            break;
        case "png":
            $ctype = "image/png";
            break;
        case "glb":
            $ctype = "model/gltf-binary";
            break;
        case "fbx":
            $ctype = "application/octet-stream";
            break;
        case "xlsx":
            $ctype = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            break;
        case "xls":
            $ctype = "application/vnd.ms-excel";
            break;
        case "dwg":
            $ctype = "application/acad";
            break;
        case "dxf":
            $ctype = "application/dxf";
            break;
        case "ifc":
            $ctype = "application/x-step";
            break;
    }
    
    // ✅ Usar el nombre original del archivo sin modificar
    $nombre_descarga = basename($ruta_fisica);
    
    header('Content-Description: File Transfer');
    header('Content-Type: ' . $ctype);
    header('Content-Disposition: attachment; filename="' . $nombre_descarga . '"');
    header('Expires: 0');
    header('Cache-Control: must-revalidate');
    header('Pragma: public');
    header('Content-Length: ' . filesize($ruta_fisica));
    
    readfile($ruta_fisica);
    exit;

} catch (Exception $e) {
    http_response_code(500);
    echo "Error: " . $e->getMessage();
}
?>