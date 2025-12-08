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

    // EDICIÓN: Agregamos d.proyecto_id a la selección
    $doc = $db->query("SELECT d.nombre, d.proyecto_id, p.nombre as proy FROM documentos d JOIN proyectos p ON d.proyecto_id = p.id WHERE d.id = $id")->fetch(PDO::FETCH_ASSOC);

    if (!$doc) {
        throw new Exception("Documento no encontrado");
    }

    $stmt = $db->prepare("UPDATE documentos SET eliminado = 1, fecha_eliminacion = NOW() WHERE id = :id");
    $stmt->execute([':id' => $id]);

    // Auditoría
    $device_info = extraerInfoDispositivo($data);

    $descripcionAuditoria = "Movió a papelera: '{$doc['nombre']}'\n";
    $descripcionAuditoria .= "Proyecto: {$doc['proy']}";

    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Eliminó (papelera) el documento '{$doc['nombre']}'",
        'documentos',
        $id,
        $device_info,
        $descripcionAuditoria
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
            // No notificar al usuario que realizó la acción si no se desea, pero el ejemplo BIM notifica a todos.
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El documento '{$doc['nombre']}' ha sido movido a la papelera.",
                ':asunto' => 'Documento eliminado',
                ':url' => "http://tu-dominio/proyectos/{$doc['proyecto_id']}" // Ajusta tu URL base
            ]);
        }
    } catch (Exception $e) {
        // Silenciamos error de notificación para no detener el flujo principal
        error_log("Error enviando notificación: " . $e->getMessage());
    }
    // ============================================

    http_response_code(200);
    echo json_encode(["success" => true, "message" => "Enviado a papelera"]);

} catch (Exception $e) {
    http_response_code(500); echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>