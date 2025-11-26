<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// SOLO importamos database.php. La línea de config_urls.php HA SIDO ELIMINADA.
require_once '../db_config/database.php';

// Verificar autenticación
$headers = getallheaders();
if (!isset($headers['Authorization'])) {
    echo json_encode(['success' => false, 'message' => 'Token no proporcionado']);
    exit;
}

// Obtener datos del JSON enviado
$data = json_decode(file_get_contents('php://input'), true);

// Validaciones
if (empty($data['proyecto_id'])) {
    echo json_encode(['success' => false, 'message' => 'proyecto_id es requerido']);
    exit;
}

if (empty($data['titulo'])) {
    echo json_encode(['success' => false, 'message' => 'titulo es requerido']);
    exit;
}

if (empty($data['fecha_hora'])) {
    echo json_encode(['success' => false, 'message' => 'fecha_hora es requerida']);
    exit;
}

if (empty($data['participantes']) || !is_array($data['participantes'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Debe seleccionar al menos un participante'
    ]);
    exit;
}

// Validar formato de fecha
$fecha_hora = $data['fecha_hora'];
$fecha_hora_fin = $data['fecha_hora_fin'] ?? null;

if (!preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $fecha_hora)) {
    echo json_encode([
        'success' => false,
        'message' => 'Formato de fecha inválido. Use: YYYY-MM-DD HH:MM:SS'
    ]);
    exit;
}

try {
    $database = new Database();
    $pdo = $database->getConnection();
    $pdo->beginTransaction();
    
    // Verificar que el proyecto existe
    $proyectoQuery = "SELECT id, nombre FROM proyectos WHERE id = :id AND eliminado = 0";
    $stmt = $pdo->prepare($proyectoQuery);
    $stmt->execute(['id' => $data['proyecto_id']]);
    $proyecto = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$proyecto) {
        $pdo->rollBack();
        echo json_encode(['success' => false, 'message' => 'Proyecto no encontrado']);
        exit;
    }
    
    // Verificar que todos los participantes existen
    $placeholders = str_repeat('?,', count($data['participantes']) - 1) . '?';
    $userQuery = "SELECT id FROM users WHERE id IN ($placeholders) AND eliminado = 0";
    $stmt = $pdo->prepare($userQuery);
    $stmt->execute($data['participantes']);
    $validUsers = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    if (count($validUsers) !== count($data['participantes'])) {
        $pdo->rollBack();
        echo json_encode(['success' => false, 'message' => 'Algunos participantes no son válidos']);
        exit;
    }
    
    // Insertar reunión
    $insertQuery = "
        INSERT INTO reuniones (
            proyecto_id, 
            titulo, 
            descripcion, 
            fecha_hora, 
            fecha_hora_fin, 
            creador_id,
            eliminado,
            created_at,
            updated_at
        ) VALUES (
            :proyecto_id, 
            :titulo, 
            :descripcion, 
            :fecha_hora, 
            :fecha_hora_fin, 
            NULL,
            0,
            NOW(),
            NOW()
        )
    ";
    
    $stmt = $pdo->prepare($insertQuery);
    $stmt->execute([
        'proyecto_id' => $data['proyecto_id'],
        'titulo' => trim($data['titulo']),
        'descripcion' => isset($data['descripcion']) ? trim($data['descripcion']) : null,
        'fecha_hora' => $fecha_hora,
        'fecha_hora_fin' => $fecha_hora_fin
    ]);
    
    $reunion_id = $pdo->lastInsertId();
    
    // Insertar participantes
    $participantesQuery = "
        INSERT INTO reuniones_usuarios (reunion_id, user_id, asistio, eliminado, created_at, updated_at) 
        VALUES (:reunion_id, :user_id, 0, 0, NOW(), NOW())
    ";
    $stmtParticipantes = $pdo->prepare($participantesQuery);
    
    foreach ($data['participantes'] as $user_id) {
        $stmtParticipantes->execute([
            'reunion_id' => $reunion_id,
            'user_id' => $user_id
        ]);
    }
    
    // Obtener la reunión creada
    $selectQuery = "
        SELECT 
            r.id,
            r.proyecto_id,
            p.nombre as proyecto_nombre,
            r.titulo,
            r.descripcion,
            r.fecha_hora,
            r.fecha_hora_fin,
            r.creador_id,
            r.eliminado
        FROM reuniones r
        LEFT JOIN proyectos p ON r.proyecto_id = p.id
        WHERE r.id = :reunion_id
    ";
    
    $stmt = $pdo->prepare($selectQuery);
    $stmt->execute(['reunion_id' => $reunion_id]);
    $reunion = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Obtener participantes
    $participantesQuery = "
        SELECT 
            ru.id,
            ru.user_id,
            CONCAT(u.name, ' ', u.apellido) as nombre,
            ru.asistio
        FROM reuniones_usuarios ru
        INNER JOIN users u ON ru.user_id = u.id
        WHERE ru.reunion_id = :reunion_id
        AND ru.eliminado = 0
    ";
    
    $stmt = $pdo->prepare($participantesQuery);
    $stmt->execute(['reunion_id' => $reunion_id]);
    $reunion['participantes'] = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // ===== NOTIFICACIONES =====
    $usuariosProyectoQuery = "
        SELECT DISTINCT user_id 
        FROM proyectos_usuarios 
        WHERE proyecto_id = :proyecto_id 
        AND eliminado = 0
    ";
    $stmt = $pdo->prepare($usuariosProyectoQuery);
    $stmt->execute(['proyecto_id' => $data['proyecto_id']]);
    $usuariosProyecto = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    $usuariosNotificar = array_unique(array_merge($data['participantes'], $usuariosProyecto));
    
    // URL es null para evitar errores
    $url = null;
    
    $notificacionQuery = "
        INSERT INTO notificaciones (
            user_id, 
            mensaje, 
            tipo, 
            asunto, 
            url,
            leida,
            eliminado,
            fecha_envio,
            created_at,
            updated_at
        ) VALUES (
            :user_id, 
            :mensaje, 
            'reunion', 
            'Nueva reunión programada', 
            :url,
            0,
            0,
            NOW(),
            NOW(),
            NOW()
        )
    ";
    
    $stmtNotificacion = $pdo->prepare($notificacionQuery);
    
    foreach ($usuariosNotificar as $user_id) {
        $mensaje = "Se ha programado una nueva reunión: '{$data['titulo']}' del proyecto '{$proyecto['nombre']}'.";
        
        $stmtNotificacion->execute([
            'user_id' => $user_id,
            'mensaje' => $mensaje,
            'url' => $url
        ]);
    }
    
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Reunión creada exitosamente',
        'data' => $reunion
    ]);
    
} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode([
        'success' => false,
        'message' => 'Error al crear reunión: ' . $e->getMessage()
    ]);
}
?>