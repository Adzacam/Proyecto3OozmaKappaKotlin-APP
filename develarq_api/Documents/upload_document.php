<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$headers = getallheaders();
$authHeader = isset($headers['Authorization']) ? $headers['Authorization'] : '';
if (empty($authHeader)) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token faltante"]); exit(); }
$token = str_replace('Bearer ', '', $authHeader);

$database = new Database();
$db = $database->getConnection();
$usuario = obtenerUsuarioDesdeToken($db, $token);

if (!$usuario) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token inválido"]); exit(); }

try {
    if (!isset($_POST['proyecto_id']) || !isset($_POST['nombre'])) throw new Exception("Datos incompletos");

    $proyecto_id = $_POST['proyecto_id'];
    $nombre = $_POST['nombre'];
    $descripcion = $_POST['descripcion'] ?? null;
    $tipo = $_POST['tipo'];
    $enlace_externo = $_POST['enlace_externo'] ?? null;
    
    $archivo_url = "";
    if ($tipo === 'URL') {
        $archivo_url = $enlace_externo; 
    } else {
        if (!isset($_FILES['archivo']) || $_FILES['archivo']['error'] !== UPLOAD_ERR_OK) throw new Exception("Error en archivo");
        
        $uploadDir = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/documentos/';
        if (!file_exists($uploadDir)) mkdir($uploadDir, 0777, true);

        $fileName = uniqid() . '_' . basename($_FILES['archivo']['name']);
        if (move_uploaded_file($_FILES['archivo']['tmp_name'], $uploadDir . $fileName)) {
            $archivo_url = 'documentos/' . $fileName;
        } else {
            throw new Exception("Error al mover archivo");
        }
    }

    $query = "INSERT INTO documentos (proyecto_id, nombre, descripcion, tipo, archivo_url, enlace_externo, subido_por, fecha_subida, created_at, updated_at) VALUES (:pid, :nom, :desc, :tipo, :url, :link, :uid, NOW(), NOW(), NOW())";
    $stmt = $db->prepare($query);
    $stmt->execute([':pid'=>$proyecto_id, ':nom'=>$nombre, ':desc'=>$descripcion, ':tipo'=>$tipo, ':url'=>$archivo_url, ':link'=>$enlace_externo, ':uid'=>$usuario['id']]);
    
    $lastId = $db->lastInsertId();
    $newDoc = $db->query("SELECT * FROM documentos WHERE id = $lastId")->fetch(PDO::FETCH_ASSOC);
    $proyecto = $db->query("SELECT nombre FROM proyectos WHERE id = $proyecto_id")->fetch(PDO::FETCH_ASSOC);
    // Auditoría
$device_info = [
    'device_model' => $_POST['device_model'] ?? null,
    'android_version' => $_POST['android_version'] ?? null,
    'sdk_version' => $_POST['sdk_version'] ?? null
];

$descripcionAuditoria = "Subió documento: '{$nombre}'\n";
$descripcionAuditoria .= "Proyecto: {$proyecto['nombre']}\n";
$descripcionAuditoria .= "Tipo: {$tipo}";

registrarAuditoriaCompleta(
    $db,
    $usuario['id'],
    "Subió el documento '{$nombre}' al proyecto '{$proyecto['nombre']}'",
    'documentos',
    $lastId,
    $device_info,
    $descripcionAuditoria
);
echo json_encode(["success" => true, "message" => "Documento subido", "data" => $newDoc]);

} catch (Exception $e) {
    http_response_code(500); echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>