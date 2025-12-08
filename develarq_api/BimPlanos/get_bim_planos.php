<?php
// ============================================
// OBTENER PLANOS BIM
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET");

include_once '../db_config/database.php';
include_once '../db_config/audit_helper.php';

$database = new Database();
$db = $database->getConnection();

// ============================================
// AUTENTICACIÓN
// ============================================
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
    echo json_encode(["success" => false, "message" => "Token inválido o usuario no encontrado"]);
    exit();
}

// ============================================
// OBTENER PARÁMETROS DE FILTRO
// ============================================
$proyecto_id = isset($_GET['proyecto_id']) ? intval($_GET['proyecto_id']) : null;
$tipo = isset($_GET['tipo']) ? $_GET['tipo'] : null;
$search = isset($_GET['search']) ? $_GET['search'] : null;
$fecha_desde = isset($_GET['fecha_desde']) ? $_GET['fecha_desde'] : null;
$fecha_hasta = isset($_GET['fecha_hasta']) ? $_GET['fecha_hasta'] : null;
$order_by = isset($_GET['order_by']) ? $_GET['order_by'] : 'fecha_subida';
$order_dir = isset($_GET['order_dir']) ? $_GET['order_dir'] : 'DESC';

// ============================================
// CONSTRUIR QUERY
// ============================================
try {
    $query = "SELECT 
                pb.id,
                pb.nombre AS titulo,
                pb.descripcion,
                pb.tipo,
                pb.archivo_url,
                pb.version,
                pb.fecha_subida,
                p.nombre AS proyecto_nombre,
                p.id AS proyecto_id,
                CONCAT(u.name, ' ', u.apellido) AS subido_por_nombre
              FROM planos_bim pb
              INNER JOIN proyectos p ON pb.proyecto_id = p.id
              LEFT JOIN users u ON pb.subido_por = u.id
              WHERE pb.eliminado = 0";
    
    // Filtros dinámicos
    if ($proyecto_id) {
        $query .= " AND pb.proyecto_id = :proyecto_id";
    }
    
    if ($tipo) {
        $query .= " AND pb.tipo = :tipo";
    }
    
    if ($search) {
        $query .= " AND (pb.nombre LIKE :search OR pb.descripcion LIKE :search)";
    }
    
    if ($fecha_desde) {
        $query .= " AND DATE(pb.fecha_subida) >= :fecha_desde";
    }
    
    if ($fecha_hasta) {
        $query .= " AND DATE(pb.fecha_subida) <= :fecha_hasta";
    }
    
    // Ordenamiento
    $query .= " ORDER BY pb.$order_by $order_dir";
    
    $stmt = $db->prepare($query);
    
    // Bind de parámetros
    if ($proyecto_id) $stmt->bindParam(':proyecto_id', $proyecto_id);
    if ($tipo) $stmt->bindParam(':tipo', $tipo);
    if ($search) {
        $search_param = "%$search%";
        $stmt->bindParam(':search', $search_param);
    }
    if ($fecha_desde) $stmt->bindParam(':fecha_desde', $fecha_desde);
    if ($fecha_hasta) $stmt->bindParam(':fecha_hasta', $fecha_hasta);
    
    $stmt->execute();
    
    $planos = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $planos[] = [
            'id' => intval($row['id']),
            'titulo' => $row['titulo'],
            'descripcion' => $row['descripcion'],
            'tipo' => $row['tipo'],
            'archivo_url' => $row['archivo_url'],
            'version' => $row['version'],
            'fecha_subida' => $row['fecha_subida'],
            'proyecto_nombre' => $row['proyecto_nombre'],
            'proyecto_id' => intval($row['proyecto_id']),
            'subido_por_nombre' => $row['subido_por_nombre']
        ];
    }
    
    echo json_encode([
        "success" => true,
        "data" => $planos
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener planos: " . $e->getMessage()
    ]);
}
?>