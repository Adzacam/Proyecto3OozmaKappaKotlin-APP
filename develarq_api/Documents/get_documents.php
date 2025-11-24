<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

require_once '../db_config/database.php';

// 1. OBTENER HEADERS (Método compatible con get_Projects.php)
$headers = getallheaders();

// Parche para XAMPP si getallheaders no trae Authorization
if (!isset($headers['Authorization']) && isset($_SERVER['HTTP_AUTHORIZATION'])) {
    $headers['Authorization'] = $_SERVER['HTTP_AUTHORIZATION'];
}

// 2. VERIFICAR SI EXISTE EL HEADER (Igual que en get_Projects.php)
if (!isset($headers['Authorization'])) {
    http_response_code(401);
    echo json_encode(["success" => false, "message" => "Token no proporcionado"]);
    exit();
}

// Limpiar el token (quitar "Bearer ")
$authHeader = $headers['Authorization'];
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
} else {
    $token = $authHeader;
}

$database = new Database();
$db = $database->getConnection();

try {
    // ---------------------------------------------------------
    // ZONA DE VALIDACIÓN (Comentada para que funcione igual que Proyectos)
    // ---------------------------------------------------------
    /* Si quieres activar la seguridad estricta más adelante, descomenta esto.
       Nota: Asegúrate de que el token que envía Android (5be01...)
       esté REALMENTE guardado en la columna 'remember_token' de la tabla 'users'.
    
    $query = "SELECT id FROM users WHERE remember_token = :token AND eliminado = 0";
    $stmt = $db->prepare($query);
    $stmt->bindParam(":token", $token);
    $stmt->execute();
    
    if ($stmt->rowCount() == 0) {
        http_response_code(401);
        echo json_encode(["success" => false, "message" => "Token inválido en BD"]);
        exit();
    }
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    $user_id = $user['id'];
    */
    // ---------------------------------------------------------

    // Filtro opcional por proyecto
    $proyecto_id = isset($_GET['proyecto_id']) ? intval($_GET['proyecto_id']) : null;
    
    // Consulta
    $query = "SELECT 
                d.id,
                d.proyecto_id,
                d.nombre,
                d.descripcion,
                d.archivo_url,
                d.enlace_externo,
                d.tipo,
                d.fecha_subida,
                d.subido_por,
                d.eliminado,
                d.fecha_eliminacion,
                p.nombre as proyecto_nombre,
                CONCAT(u.name, ' ', u.apellido) as subido_por_nombre
              FROM documentos d
              INNER JOIN proyectos p ON d.proyecto_id = p.id
              LEFT JOIN users u ON d.subido_por = u.id
              WHERE d.eliminado = 0";
    
    if ($proyecto_id) {
        $query .= " AND d.proyecto_id = :proyecto_id";
    }
    
    $query .= " ORDER BY d.fecha_subida DESC";
    
    $stmt = $db->prepare($query);
    
    if ($proyecto_id) {
        $stmt->bindParam(":proyecto_id", $proyecto_id);
    }
    
    $stmt->execute();
    $documents = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    http_response_code(200);
    echo json_encode([
        "success" => true,
        "data" => $documents,
        "message" => "Documentos obtenidos exitosamente"
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener documentos: " . $e->getMessage()
    ]);
}
?>