<?php
// ============================================
// ACTUALIZAR PLANO BIM
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// AUTENTICACIÓN
$headers = getallheaders();
$token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

if (!$token) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

$usuario = obtenerUsuarioDesdeToken($db, $token);
if (!$usuario) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// RECIBIR DATOS
$data = json_decode(file_get_contents("php://input"));
$plano_id = isset($data->id) ? intval($data->id) : null;
$nombre = isset($data->nombre) ? trim($data->nombre) : null;
$descripcion = isset($data->descripcion) ? trim($data->descripcion) : null;
$device_info = extraerInfoDispositivo($data);

if (!$plano_id || !$nombre) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Datos incompletos"]);
    exit();
}

// ACTUALIZAR
try {
    $query = "UPDATE planos_bim 
              SET nombre = :nombre, descripcion = :descripcion, updated_at = NOW() 
              WHERE id = :id";
    
    $stmt = $db->prepare($query);
    $stmt->execute([
        ':nombre' => $nombre,
        ':descripcion' => $descripcion,
        ':id' => $plano_id
    ]);
    
    // AUDITORÍA
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Actualizó el plano BIM '$nombre'",
        'planos_bim',
        $plano_id,
        $device_info
    );
    
    echo json_encode([
        "success" => true,
        "message" => "Plano actualizado correctamente"
    ]);
    // ============================================
    // ENVIAR NOTIFICACIONES
    // ============================================
    try {
        $query_proyecto = "SELECT proyecto_id FROM planos_bim WHERE id = :id";
        $stmt_proy = $db->prepare($query_proyecto);
        $stmt_proy->execute([':id' => $plano_id]);
        $proyecto = $stmt_proy->fetch(PDO::FETCH_ASSOC);

        if ($proyecto) {
            $query_usuarios = "SELECT DISTINCT user_id 
                            FROM proyectos_usuarios 
                            WHERE proyecto_id = :proyecto_id";
            
            $stmt_usuarios = $db->prepare($query_usuarios);
            $stmt_usuarios->execute([':proyecto_id' => $proyecto['proyecto_id']]);

            $query_notif = "INSERT INTO notificaciones 
                            (user_id, mensaje, tipo, asunto, url, created_at) 
                            VALUES 
                            (:user_id, :mensaje, 'documento', :asunto, :url, NOW())";
            
            $stmt_notif = $db->prepare($query_notif);

            while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
                $stmt_notif->execute([
                    ':user_id' => $row['user_id'],
                    ':mensaje' => "El plano BIM '{$nombre}' ha sido actualizado.",
                    ':asunto' => 'Plano actualizado',
                    ':url' => "http://127.0.0.1:8000/proyectos/{$proyecto['proyecto_id']}"
                ]);
            }
        }

} catch (Exception $e) {
    error_log("Error al enviar notificaciones: " . $e->getMessage());
}
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>