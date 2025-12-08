<?php
// ============================================
// OBTENER TIMELINE DEL PROYECTO
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
    echo json_encode(["success" => false, "message" => "Token inválido"]);
    exit();
}

// ============================================
// OBTENER PARÁMETROS
// ============================================
$proyecto_id = isset($_GET['proyecto_id']) ? intval($_GET['proyecto_id']) : null;

if (!$proyecto_id) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "ID de proyecto requerido"]);
    exit();
}

// ============================================
// CONSTRUIR TIMELINE
// ============================================
try {
    $timeline = [];

    // ============================================
    // 1. EVENTOS DE AUDITORÍA
    // ============================================
    $query_auditoria = "SELECT 
                        id,
                        'auditoria' as tipo,
                        accion as titulo,
                        descripcion_detallada as descripcion,
                        fecha_accion as fecha,
                        user_id,
                        tabla_afectada,
                        id_registro_afectado as registro_id
                        FROM auditoria_logs
                        WHERE id_registro_afectado = :proyecto_id
                        AND tabla_afectada IN ('proyectos', 'proyectos_usuarios', 'hitos')
                        AND eliminado = 0
                        ORDER BY fecha_accion DESC";
    
    $stmt_auditoria = $db->prepare($query_auditoria);
    $stmt_auditoria->execute([':proyecto_id' => $proyecto_id]);
    
    while ($row = $stmt_auditoria->fetch(PDO::FETCH_ASSOC)) {
        // Obtener nombre del usuario
        $query_user = "SELECT CONCAT(name, ' ', apellido) as nombre FROM users WHERE id = :user_id";
        $stmt_user = $db->prepare($query_user);
        $stmt_user->execute([':user_id' => $row['user_id']]);
        $user_data = $stmt_user->fetch(PDO::FETCH_ASSOC);
        
        $timeline[] = [
            'id' => intval($row['id']),
            'tipo' => $row['tipo'],
            'titulo' => $row['titulo'],
            'descripcion' => $row['descripcion'],
            'fecha' => $row['fecha'],
            'usuario_nombre' => $user_data ? $user_data['nombre'] : 'Usuario desconocido',
            'tabla_afectada' => $row['tabla_afectada'],
            'registro_id' => $row['registro_id'] ? intval($row['registro_id']) : null
        ];
    }

    // ============================================
    // 2. DOCUMENTOS SUBIDOS
    // ============================================
    $query_docs = "SELECT 
                   d.id,
                   'documento' as tipo,
                   CONCAT('Documento: ', d.nombre) as titulo,
                   d.descripcion,
                   d.fecha_subida as fecha,
                   CONCAT(u.name, ' ', u.apellido) as usuario_nombre
                   FROM documentos d
                   LEFT JOIN users u ON d.subido_por = u.id
                   WHERE d.proyecto_id = :proyecto_id AND d.eliminado = 0
                   ORDER BY d.fecha_subida DESC";
    
    $stmt_docs = $db->prepare($query_docs);
    $stmt_docs->execute([':proyecto_id' => $proyecto_id]);
    
    while ($row = $stmt_docs->fetch(PDO::FETCH_ASSOC)) {
        $timeline[] = [
            'id' => intval($row['id']),
            'tipo' => $row['tipo'],
            'titulo' => $row['titulo'],
            'descripcion' => $row['descripcion'],
            'fecha' => $row['fecha'],
            'usuario_nombre' => $row['usuario_nombre'],
            'tabla_afectada' => 'documentos',
            'registro_id' => intval($row['id'])
        ];
    }

    // ============================================
    // 3. PLANOS BIM
    // ============================================
    $query_bim = "SELECT 
                  pb.id,
                  'bim' as tipo,
                  CONCAT('Plano BIM: ', pb.nombre) as titulo,
                  pb.descripcion,
                  pb.fecha_subida as fecha,
                  CONCAT(u.name, ' ', u.apellido) as usuario_nombre
                  FROM planos_bim pb
                  LEFT JOIN users u ON pb.subido_por = u.id
                  WHERE pb.proyecto_id = :proyecto_id AND pb.eliminado = 0
                  ORDER BY pb.fecha_subida DESC";
    
    $stmt_bim = $db->prepare($query_bim);
    $stmt_bim->execute([':proyecto_id' => $proyecto_id]);
    
    while ($row = $stmt_bim->fetch(PDO::FETCH_ASSOC)) {
        $timeline[] = [
            'id' => intval($row['id']),
            'tipo' => $row['tipo'],
            'titulo' => $row['titulo'],
            'descripcion' => $row['descripcion'],
            'fecha' => $row['fecha'],
            'usuario_nombre' => $row['usuario_nombre'],
            'tabla_afectada' => 'planos_bim',
            'registro_id' => intval($row['id'])
        ];
    }

    // ============================================
    // 4. REUNIONES
    // ============================================
    $query_reuniones = "SELECT 
                        r.id,
                        'reunion' as tipo,
                        CONCAT('Reunión: ', r.titulo) as titulo,
                        r.descripcion,
                        r.created_at as fecha,
                        CONCAT(u.name, ' ', u.apellido) as usuario_nombre
                        FROM reuniones r
                        LEFT JOIN users u ON r.creador_id = u.id
                        WHERE r.proyecto_id = :proyecto_id AND r.eliminado = 0
                        ORDER BY r.created_at DESC";
    
    $stmt_reuniones = $db->prepare($query_reuniones);
    $stmt_reuniones->execute([':proyecto_id' => $proyecto_id]);
    
    while ($row = $stmt_reuniones->fetch(PDO::FETCH_ASSOC)) {
        $timeline[] = [
            'id' => intval($row['id']),
            'tipo' => $row['tipo'],
            'titulo' => $row['titulo'],
            'descripcion' => $row['descripcion'],
            'fecha' => $row['fecha'],
            'usuario_nombre' => $row['usuario_nombre'],
            'tabla_afectada' => 'reuniones',
            'registro_id' => intval($row['id'])
        ];
    }

    // ============================================
    // ORDENAR POR FECHA DESCENDENTE
    // ============================================
    usort($timeline, function($a, $b) {
        return strtotime($b['fecha']) - strtotime($a['fecha']);
    });

    echo json_encode([
        "success" => true,
        "data" => $timeline
    ]);
    
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener timeline: " . $e->getMessage()
    ]);
}
?>