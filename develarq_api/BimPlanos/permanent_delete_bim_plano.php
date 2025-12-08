<?php
// ============================================
// ELIMINAR PERMANENTEMENTE PLANO BIM
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

// ✅ VERIFICAR QUE SEA ADMIN
if (strtolower($usuario['rol']) !== 'admin') {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "Solo administradores pueden eliminar permanentemente"]);
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

// ✅ ELIMINAR PERMANENTEMENTE
try {
    // Obtener info del plano ANTES de eliminar
    $query_info = "SELECT nombre, archivo_url, proyecto_id FROM planos_bim WHERE id = :id AND eliminado = 1";
    $stmt_info = $db->prepare($query_info);
    $stmt_info->execute([':id' => $plano_id]);
    $plano = $stmt_info->fetch(PDO::FETCH_ASSOC);
    
    if (!$plano) {
        http_response_code(404);
        echo json_encode(["success" => false, "message" => "Plano no encontrado en papelera"]);
        exit();
    }
    
    // ✅ ELIMINAR ARCHIVO FÍSICO DEL SERVIDOR
    if (!empty($plano['archivo_url']) && !filter_var($plano['archivo_url'], FILTER_VALIDATE_URL)) {
        $ruta_relativa = str_replace('/storage/', '', $plano['archivo_url']);
        $ruta_fisica = 'C:/Users/HP/Documents/Univalle/6to Semestre proyecto Develarq/Proyecto3OozmaKappa/storage/app/public/' . $ruta_relativa;
        
        if (file_exists($ruta_fisica)) {
            unlink($ruta_fisica);
        }
    }
    
    // ✅ ELIMINAR REGISTRO DE LA BASE DE DATOS
    $query = "DELETE FROM planos_bim WHERE id = :id";
    $stmt = $db->prepare($query);
    $stmt->execute([':id' => $plano_id]);
    
    // AUDITORÍA
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Eliminó PERMANENTEMENTE el plano BIM '{$plano['nombre']}'",
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
                ':mensaje' => "El plano BIM '{$plano['nombre']}' fue eliminado permanentemente por un administrador.",
                ':asunto' => 'Plano eliminado permanentemente',
                ':url' => "http://127.0.0.1:8000/proyectos/{$plano['proyecto_id']}"
            ]);
        }

    } catch (Exception $e) {
        error_log("Error al enviar notificaciones: " . $e->getMessage());
    }
    
    echo json_encode([
        "success" => true,
        "message" => "Plano eliminado permanentemente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>