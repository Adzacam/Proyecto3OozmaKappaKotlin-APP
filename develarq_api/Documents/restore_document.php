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
    $data = json_decode(file_get_contents("php://input"));
    $id = intval($data->id);

    // EDICIÓN: Obtener proyecto_id
    $doc = $db->query("SELECT nombre, proyecto_id FROM documentos WHERE id = $id")->fetch(PDO::FETCH_ASSOC);

    if (!$doc) throw new Exception("Documento no encontrado");

    $stmt = $db->prepare("UPDATE documentos SET eliminado = 0, fecha_eliminacion = NULL WHERE id = :id");
    $stmt->execute([':id' => $id]);

    // Auditoría
    $device_info = extraerInfoDispositivo($data);
    $descripcionAuditoria = "Restauró documento: '{$doc['nombre']}' de la papelera";

    registrarAuditoriaCompleta(
        $db, $usuario['id'], "Restauró el documento '{$doc['nombre']}'",
        'documentos', $id, $device_info, $descripcionAuditoria
    );

    // ============================================
    // NUEVA LÓGICA DE NOTIFICACIONES
    // ============================================
    try {
        $query_usuarios = "SELECT DISTINCT user_id FROM proyectos_usuarios WHERE proyecto_id = :proyecto_id AND eliminado = 0";
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':proyecto_id' => $doc['proyecto_id']]);

        $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at) VALUES (:user_id, :mensaje, 'documento', :asunto, :url, NOW())";
        $stmt_notif = $db->prepare($query_notif);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El documento '{$doc['nombre']}' ha sido restaurado.",
                ':asunto' => 'Documento restaurado',
                ':url' => "http://tu-dominio/proyectos/{$doc['proyecto_id']}"
            ]);
        }
    } catch (Exception $e) {
        error_log("Error notificación: " . $e->getMessage());
    }
    // ============================================

    http_response_code(200);
    echo json_encode(["success" => true, "message" => "Documento restaurado"]);

} catch (Exception $e) {
    http_response_code(500); echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>