<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') { http_response_code(200); exit(); }

require_once '../db_config/database.php';
require_once '../db_config/audit_helper.php';

$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? '';
$token = str_replace('Bearer ', '', $authHeader);

$database = new Database(); $db = $database->getConnection();
$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) { http_response_code(401); echo json_encode(["success" => false, "message" => "Token inválido"]); exit(); }

try {
    $data = json_decode(file_get_contents("php://input")); // Ahora leemos el body

    $docs = $db->query("SELECT id, nombre, archivo_url, tipo FROM documentos WHERE eliminado = 1 AND DATEDIFF(NOW(), fecha_eliminacion) >= 30")->fetchAll(PDO::FETCH_ASSOC);
    
    if (empty($docs)) { echo json_encode(["success"=>true, "message"=>"Nada que purgar", "deleted_count"=>0]); exit(); }

    $count = 0;
    foreach ($docs as $doc) {
        if ($doc['tipo'] !== 'URL' && !empty($doc['archivo_url'])) {
            $path = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/' . $doc['archivo_url'];
            if (file_exists($path)) unlink($path);
        }
        $db->prepare("DELETE FROM documentos WHERE id = ?")->execute([$doc['id']]);
        $count++;
    }

    // Auditoría
        $device_info = extraerInfoDispositivo($data);

    $descripcionAuditoria = "Purgó $count documentos antiguos de la papelera (30+ días)";

    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Purgó papelera ($count documentos)",
        'documentos',
        0, // Sin ID específico
        $device_info,
        $descripcionAuditoria
    );
    http_response_code(200);
    echo json_encode(["success" => true, "message" => "Purga completada", "deleted_count" => $count]);

} catch (Exception $e) {
    http_response_code(500); echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>