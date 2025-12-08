<?php
// ============================================
// OBTENER PLANOS EN PAPELERA
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

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

// OBTENER PLANOS ELIMINADOS
try {
    $query = "SELECT 
                pb.id,
                pb.nombre AS titulo,
                pb.tipo,
                p.nombre AS proyecto_nombre,
                pb.updated_at AS eliminado_el,
                DATEDIFF(DATE_ADD(pb.updated_at, INTERVAL 30 DAY), NOW()) AS dias_restantes
              FROM planos_bim pb
              INNER JOIN proyectos p ON pb.proyecto_id = p.id
              WHERE pb.eliminado = 1
              ORDER BY pb.updated_at DESC";
    
    $stmt = $db->prepare($query);
    $stmt->execute();
    
    $planos_eliminados = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $planos_eliminados[] = [
            'id' => intval($row['id']),
            'titulo' => $row['titulo'],
            'tipo' => $row['tipo'],
            'proyecto_nombre' => $row['proyecto_nombre'],
            'eliminado_el' => $row['eliminado_el'],
            'dias_restantes' => intval($row['dias_restantes'])
        ];
    }
    
    echo json_encode([
        "success" => true,
        "data" => $planos_eliminados
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error: " . $e->getMessage()
    ]);
}
?>