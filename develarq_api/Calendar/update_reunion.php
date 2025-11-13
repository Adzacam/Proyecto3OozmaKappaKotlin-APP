<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, PUT');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

require_once '../db_config/database.php';

// Verificar autenticación
$headers = getallheaders();
if (!isset($headers['Authorization'])) {
    echo json_encode(['success' => false, 'message' => 'Token no proporcionado']);
    exit;
}

// Obtener ID desde URL
$reunion_id = isset($_GET['id']) ? intval($_GET['id']) : 0;

if ($reunion_id <= 0) {
    echo json_encode(['success' => false, 'message' => 'ID de reunión inválido']);
    exit;
}

// Obtener datos del JSON
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

// Validar formato de fecha
if (!preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $data['fecha_hora'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Formato de fecha inválido. Use: YYYY-MM-DD HH:MM:SS'
    ]);
    exit;
}

try {
    $pdo = $database->getConnection();
    $pdo->beginTransaction();
    
    // Verificar que la reunión existe
    $checkQuery = "SELECT id, titulo, proyecto_id FROM reuniones WHERE id = :id AND eliminado = 0";
    $stmt = $pdo->prepare($checkQuery);
    $stmt->execute(['id' => $reunion_id]);
    $reunionAnterior = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$reunionAnterior) {
        $pdo->rollBack();
        echo json_encode(['success' => false, 'message' => 'Reunión no encontrada']);
        exit;
    }
    
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
    
    // Actualizar reunión
    $updateQuery = "
        UPDATE reuniones SET
            proyecto_id = :proyecto_id,
            titulo = :titulo,
            descripcion = :descripcion,
            fecha_hora = :fecha_hora,
            fecha_hora_fin = :fecha_hora_fin,
            updated_at = NOW()
        WHERE id = :id
    ";
    
    $stmt = $pdo->prepare($updateQuery);
    $stmt->execute([
        'id' => $reunion_id,
        'proyecto_id' => $data['proyecto_id'],
        'titulo' => trim($data['titulo']),
        'descripcion' => isset($data['descripcion']) ? trim($data['descripcion']) : null,
        'fecha_hora' => $data['fecha_hora'],
        'fecha_hora_fin' => $data['fecha_hora_fin'] ?? null
    ]);
    
    // Actualizar participantes si se proporcionan
    if (isset($data['participantes']) && is_array($data['participantes']) && !empty($data['participantes'])) {
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
        
        // Marcar participantes anteriores como eliminados
        $deleteParticipantesQuery = "
            UPDATE reuniones_usuarios 
            SET eliminado = 1, updated_at = NOW()
            WHERE reunion_id = :reunion_id
        ";
        $stmt = $pdo->prepare($deleteParticipantesQuery);
        $stmt->execute(['reunion_id' => $reunion_id]);
        
        // Insertar nuevos participantes
        $insertParticipantesQuery = "
            INSERT INTO reuniones_usuarios (reunion_id, user_id, asistio, eliminado, created_at, updated_at) 
            VALUES (:reunion_id, :user_id, 0, 0, NOW(), NOW())
        ";
        $stmtParticipantes = $pdo->prepare($insertParticipantesQuery);
        
        foreach ($data['participantes'] as $user_id) {
            $stmtParticipantes->execute([
                'reunion_id' => $reunion_id,
                'user_id' => $user_id
            ]);
        }
    }
    
    // Obtener la reunión actualizada
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
    
    // Obtener participantes actualizados
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
    // Notificar a participantes sobre la actualización
    if (isset($data['participantes']) && is_array($data['participantes'])) {
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
                'Reunión actualizada', 
                :url,
                0,
                0,
                NOW(),
                NOW(),
                NOW()
            )
        ";
        
        $stmtNotificacion = $pdo->prepare($notificacionQuery);
        
        foreach ($data['participantes'] as $user_id) {
            $mensaje = "La reunión '{$data['titulo']}' del proyecto '{$proyecto['nombre']}' ha sido actualizada.";
            $url = "http://127.0.0.1:8000/proyectos/{$data['proyecto_id']}";
            
            $stmtNotificacion->execute([
                'user_id' => $user_id,
                'mensaje' => $mensaje,
                'url' => $url
            ]);
        }
    }
    
    $pdo->commit();
    
    echo json_encode([
        'success' => true,
        'message' => 'Reunión actualizada exitosamente',
        'data' => $reunion
    ]);
    
} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode([
        'success' => false,
        'message' => 'Error al actualizar reunión: ' . $e->getMessage()
    ]);
}
?>