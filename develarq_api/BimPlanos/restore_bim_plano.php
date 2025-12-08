<?php
// ============================================
// RESTAURAR PLANO DESDE PAPELERA
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
$device_info = extraerInfoDispositivo($data);

if (!$plano_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID del plano no proporcionado"]);
    exit();
}

// RESTAURAR
try {
    // Obtener nombre del plano
    $query_info = "SELECT nombre, proyecto_id FROM planos_bim WHERE id = :id AND eliminado = 1";
    $stmt_info = $db->prepare($query_info);
    $stmt_info->execute([':id' => $plano_id]);
    $plano = $stmt_info->fetch(PDO::FETCH_ASSOC);
    
    if (!$plano) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Plano no encontrado en papelera"]);
        exit();
    }
    
    // ✅ RESTAURAR (eliminado = 0)
    $query = "UPDATE planos_bim 
              SET eliminado = 0, updated_at = NOW() 
              WHERE id = :id";
    
    $stmt = $db->prepare($query);
    $stmt->execute([':id' => $plano_id]);
    
    // AUDITORÍA
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Restauró el plano BIM '{$plano['nombre']}'",
        'planos_bim',
        $plano_id,
        $device_info
    );
    
    // ============================================
    // ENVIAR NOTIFICACIONES
    // ============================================
    try {
        $query_usuarios = "SELECT DISTINCT user_id 
                          FROM proyectos_usuarios 
                          WHERE proyecto_id = :proyecto_id";
        
        $stmt_usuarios = $db->prepare($query_usuarios);
        $stmt_usuarios->execute([':proyecto_id' => $plano['proyecto_id']]);

        $query_notif = "INSERT INTO notificaciones 
                        (user_id, mensaje, tipo, asunto, url, created_at) 
                        VALUES 
                        (:user_id, :mensaje, 'documento', :asunto, :url, NOW())";
        
        $stmt_notif = $db->prepare($query_notif);

        while ($row = $stmt_usuarios->fetch(PDO::FETCH_ASSOC)) {
            $stmt_notif->execute([
                ':user_id' => $row['user_id'],
                ':mensaje' => "El plano BIM '{$plano['nombre']}' fue restaurado.",
                ':asunto' => 'Plano restaurado',
                ':url' => "http://127.0.0.1:8000/proyectos/{$plano['proyecto_id']}"
            ]);
        }

    } catch (Exception $e) {
        error_log("Error al enviar notificaciones: " . $e->getMessage());
    }
    
    echo json_encode([
        "success" => true,
        "message" => "Plano restaurado correctamente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>