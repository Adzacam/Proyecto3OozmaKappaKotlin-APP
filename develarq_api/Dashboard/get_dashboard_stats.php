<?php
// Dashboard/get_dashboard_stats.php

header('Content-Type: application/json; charset=utf-8');
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once '../db_config/database.php';

try {
    $database = new Database();
    $db = $database->getConnection();

    // Obtener token del header
    $headers = getallheaders();
    $token = isset($headers['Authorization']) ? str_replace('Bearer ', '', $headers['Authorization']) : null;

    if (!$token) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Token no proporcionado"
        ]);
        exit();
    }

    // Verificar token y obtener usuario
    $query = "SELECT id, name, apellido, rol FROM users 
              WHERE remember_token = :token AND eliminado = 0 LIMIT 1";
    $stmt = $db->prepare($query);
    $stmt->execute([':token' => $token]);
    
    if ($stmt->rowCount() === 0) {
        http_response_code(401);
        echo json_encode([
            "success" => false,
            "message" => "Token inválido"
        ]);
        exit();
    }

    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    $user_id = $user['id'];
    $rol = strtolower($user['rol']);

    $stats = [
        'user_info' => [
            'id' => (int)$user['id'],
            'nombre' => $user['name'] . ' ' . $user['apellido'],
            'rol' => $user['rol']
        ]
    ];

    // =========================================
    // ESTADÍSTICAS SEGÚN ROL
    // =========================================

    if ($rol === 'admin') {
        // ========== ADMIN: MIS ASIGNACIONES PRIMERO ==========
        
        // Mis proyectos
        // NOTA: Usamos responsable_id ya que creado_por no existe en tu tabla proyectos según el SQL
        $queryMisProyectos = "SELECT COUNT(DISTINCT p.id) as total 
                              FROM proyectos p
                              LEFT JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id AND pu.user_id = :user_id
                              WHERE p.eliminado = 0 
                              AND (p.responsable_id = :user_id OR pu.user_id = :user_id)";
        $stmtMisProyectos = $db->prepare($queryMisProyectos);
        $stmtMisProyectos->execute([':user_id' => $user_id]);
        $stats['mis_proyectos'] = (int)$stmtMisProyectos->fetch(PDO::FETCH_ASSOC)['total'];

        // Mis tareas asignadas
        $queryMisTareas = "SELECT COUNT(*) as total 
                           FROM tareas 
                           WHERE asignado_id = :user_id 
                           AND eliminado = 0";
        $stmtMisTareas = $db->prepare($queryMisTareas);
        $stmtMisTareas->execute([':user_id' => $user_id]);
        $stats['mis_tareas'] = (int)$stmtMisTareas->fetch(PDO::FETCH_ASSOC)['total'];

        // Mis tareas por estado
        $queryMisTareasEstado = "SELECT estado, COUNT(*) as cantidad 
                                 FROM tareas 
                                 WHERE asignado_id = :user_id 
                                 AND eliminado = 0 
                                 GROUP BY estado";
        $stmtMisTareasEstado = $db->prepare($queryMisTareasEstado);
        $stmtMisTareasEstado->execute([':user_id' => $user_id]);
        $stats['mis_tareas_por_estado'] = $stmtMisTareasEstado->fetchAll(PDO::FETCH_ASSOC);

        // Mis reuniones próximas
        $queryMisReuniones = "SELECT COUNT(DISTINCT r.id) as total 
                              FROM reuniones r
                              INNER JOIN reuniones_usuarios ru ON r.id = ru.reunion_id
                              WHERE ru.user_id = :user_id 
                              AND r.eliminado = 0 
                              AND r.fecha_hora >= NOW()
                              AND r.fecha_hora <= DATE_ADD(NOW(), INTERVAL 7 DAY)";
        $stmtMisReuniones = $db->prepare($queryMisReuniones);
        $stmtMisReuniones->execute([':user_id' => $user_id]);
        $stats['mis_reuniones'] = (int)$stmtMisReuniones->fetch(PDO::FETCH_ASSOC)['total'];

        // Mis tareas pendientes (próximas 5)
        // CORRECCIÓN: fecha_vencimiento -> fecha_limite
        
        $queryTareasPendientes = "SELECT id, titulo, estado, prioridad, fecha_limite as fecha_vencimiento, " .
                                  "(SELECT nombre FROM proyectos WHERE id = tareas.proyecto_id) as proyecto_nombre " .
                                  "FROM tareas " .
                                  "WHERE asignado_id = :user_id " .
                                  "AND eliminado = 0 " .
                                  "AND estado IN ('pendiente', 'en_progreso') " .
                                  "ORDER BY " .
                                    "CASE prioridad " .
                                      "WHEN 'alta' THEN 1 " .
                                      "WHEN 'media' THEN 2 " .
                                      "WHEN 'baja' THEN 3 " .
                                    "END, " .
                                    "fecha_limite ASC " .
                                  "LIMIT 5";
        $stmtTareasPendientes = $db->prepare($queryTareasPendientes);
        $stmtTareasPendientes->execute([':user_id' => $user_id]);
        $stats['tareas_pendientes'] = $stmtTareasPendientes->fetchAll(PDO::FETCH_ASSOC);

        // Mis próximas reuniones (próximas 3)
        $queryProximasReuniones = "SELECT r.id, r.titulo, r.fecha_hora, r.ubicacion,
                                   (SELECT nombre FROM proyectos WHERE id = r.proyecto_id) as proyecto_nombre
                                   FROM reuniones r
                                   INNER JOIN reuniones_usuarios ru ON r.id = ru.reunion_id
                                   WHERE ru.user_id = :user_id 
                                   AND r.eliminado = 0 
                                   AND r.fecha_hora >= NOW()
                                   ORDER BY r.fecha_hora ASC
                                   LIMIT 3";
        // Nota: La columna ubicacion no está en tu SQL, si da error bórrala de la select. 
        // Según tu SQL no existe 'ubicacion' en tabla reuniones, la voy a quitar para prevenir otro error 500.
        $queryProximasReuniones = "SELECT r.id, r.titulo, r.fecha_hora, 
                                   (SELECT nombre FROM proyectos WHERE id = r.proyecto_id) as proyecto_nombre
                                   FROM reuniones r
                                   INNER JOIN reuniones_usuarios ru ON r.id = ru.reunion_id
                                   WHERE ru.user_id = :user_id 
                                   AND r.eliminado = 0 
                                   AND r.fecha_hora >= NOW()
                                   ORDER BY r.fecha_hora ASC
                                   LIMIT 3";
        $stmtProximasReuniones = $db->prepare($queryProximasReuniones);
        $stmtProximasReuniones->execute([':user_id' => $user_id]);
        $stats['proximas_reuniones'] = $stmtProximasReuniones->fetchAll(PDO::FETCH_ASSOC);

        // ========== ADMIN: VISTA GENERAL DEL SISTEMA ==========
        
        // Total de proyectos en el sistema
        $queryProyectos = "SELECT COUNT(*) as total FROM proyectos WHERE eliminado = 0";
        $stmtProyectos = $db->prepare($queryProyectos);
        $stmtProyectos->execute();
        $stats['total_proyectos'] = (int)$stmtProyectos->fetch(PDO::FETCH_ASSOC)['total'];

        // Proyectos por estado
        $queryEstados = "SELECT estado, COUNT(*) as cantidad 
                         FROM proyectos 
                         WHERE eliminado = 0 
                         GROUP BY estado";
        $stmtEstados = $db->prepare($queryEstados);
        $stmtEstados->execute();
        $stats['proyectos_por_estado'] = $stmtEstados->fetchAll(PDO::FETCH_ASSOC);

        // Total de tareas del sistema
        $queryTareas = "SELECT COUNT(*) as total FROM tareas WHERE eliminado = 0";
        $stmtTareas = $db->prepare($queryTareas);
        $stmtTareas->execute();
        $stats['total_tareas'] = (int)$stmtTareas->fetch(PDO::FETCH_ASSOC)['total'];

        // Tareas por estado del sistema
        $queryTareasEstado = "SELECT estado, COUNT(*) as cantidad 
                              FROM tareas 
                              WHERE eliminado = 0 
                              GROUP BY estado";
        $stmtTareasEstado = $db->prepare($queryTareasEstado);
        $stmtTareasEstado->execute();
        $stats['tareas_por_estado'] = $stmtTareasEstado->fetchAll(PDO::FETCH_ASSOC);

        // Reuniones próximas del sistema
        $queryReuniones = "SELECT COUNT(*) as total 
                           FROM reuniones 
                           WHERE eliminado = 0 
                           AND fecha_hora >= NOW()
                           AND fecha_hora <= DATE_ADD(NOW(), INTERVAL 7 DAY)";
        $stmtReuniones = $db->prepare($queryReuniones);
        $stmtReuniones->execute();
        $stats['reuniones_proximas'] = (int)$stmtReuniones->fetch(PDO::FETCH_ASSOC)['total'];

        // Documentos totales
        $queryDocs = "SELECT COUNT(*) as total FROM documentos WHERE eliminado = 0";
        $stmtDocs = $db->prepare($queryDocs);
        $stmtDocs->execute();
        $stats['total_documentos'] = (int)$stmtDocs->fetch(PDO::FETCH_ASSOC)['total'];

        // Usuarios activos
        $queryUsers = "SELECT COUNT(*) as total FROM users 
                       WHERE eliminado = 0 AND estado = 'activo'";
        $stmtUsers = $db->prepare($queryUsers);
        $stmtUsers->execute();
        $stats['usuarios_activos'] = (int)$stmtUsers->fetch(PDO::FETCH_ASSOC)['total'];

        // Actividad reciente (Últimas 5 acciones)
        $queryActividad = "SELECT 
                            al.accion,
                            al.fecha_accion,
                            u.name,
                            u.apellido
                           FROM auditoria_logs al
                           LEFT JOIN users u ON al.user_id = u.id
                           WHERE al.eliminado = 0
                           ORDER BY al.fecha_accion DESC
                           LIMIT 5";
        $stmtActividad = $db->prepare($queryActividad);
        $stmtActividad->execute();
        $stats['actividad_reciente'] = $stmtActividad->fetchAll(PDO::FETCH_ASSOC);

    } elseif (in_array($rol, ['ingeniero', 'arquitecto'])) {
        // ========== INGENIERO/ARQUITECTO: SOLO SUS ASIGNACIONES ==========
        
        // Proyectos asignados
        $queryProyectos = "SELECT COUNT(DISTINCT p.id) as total 
                           FROM proyectos p
                           INNER JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id
                           WHERE pu.user_id = :user_id 
                           AND p.eliminado = 0 
                           AND pu.eliminado = 0";
        $stmtProyectos = $db->prepare($queryProyectos);
        $stmtProyectos->execute([':user_id' => $user_id]);
        $stats['total_proyectos'] = (int)$stmtProyectos->fetch(PDO::FETCH_ASSOC)['total'];

        // Lista de proyectos asignados (con detalles)
        $queryProyectosAsignados = "SELECT DISTINCT p.id, p.nombre, p.estado, 
                                    0 as progreso,
                                    p.fecha_inicio, p.fecha_fin
                                    FROM proyectos p
                                    INNER JOIN proyectos_usuarios pu ON p.id = pu.proyecto_id
                                    WHERE pu.user_id = :user_id 
                                    AND p.eliminado = 0 
                                    AND pu.eliminado = 0
                                    ORDER BY p.fecha_inicio DESC
                                    LIMIT 5";
        $stmtProyectosAsignados = $db->prepare($queryProyectosAsignados);
        $stmtProyectosAsignados->execute([':user_id' => $user_id]);
        $stats['proyectos_asignados'] = $stmtProyectosAsignados->fetchAll(PDO::FETCH_ASSOC);

        // Tareas asignadas
        $queryTareas = "SELECT COUNT(*) as total 
                        FROM tareas 
                        WHERE asignado_id = :user_id 
                        AND eliminado = 0";
        $stmtTareas = $db->prepare($queryTareas);
        $stmtTareas->execute([':user_id' => $user_id]);
        $stats['total_tareas'] = (int)$stmtTareas->fetch(PDO::FETCH_ASSOC)['total'];

        // Tareas por estado
        $queryTareasEstado = "SELECT estado, COUNT(*) as cantidad 
                              FROM tareas 
                              WHERE asignado_id = :user_id 
                              AND eliminado = 0 
                              GROUP BY estado";
        $stmtTareasEstado = $db->prepare($queryTareasEstado);
        $stmtTareasEstado->execute([':user_id' => $user_id]);
        $stats['tareas_por_estado'] = $stmtTareasEstado->fetchAll(PDO::FETCH_ASSOC);

        // Tareas pendientes
        // CORRECCIÓN: fecha_vencimiento -> fecha_limite
        $queryTareasPendientes = "SELECT id, titulo, estado, prioridad, fecha_limite as fecha_vencimiento, " .
                                  "(SELECT nombre FROM proyectos WHERE id = tareas.proyecto_id) as proyecto_nombre " .
                                  "FROM tareas " .
                                  "WHERE asignado_id = :user_id " .
                                  "AND eliminado = 0 " .
                                  "AND estado IN ('pendiente', 'en_progreso') " .
                                  "ORDER BY fecha_limite ASC " .
                                  "LIMIT 5";
        $stmtTareasPendientes = $db->prepare($queryTareasPendientes);
        $stmtTareasPendientes->execute([':user_id' => $user_id]);
        $stats['tareas_pendientes'] = $stmtTareasPendientes->fetchAll(PDO::FETCH_ASSOC);

        // Reuniones próximas
        $queryReuniones = "SELECT COUNT(DISTINCT r.id) as total 
                           FROM reuniones r
                           INNER JOIN reuniones_usuarios ru ON r.id = ru.reunion_id
                           WHERE ru.user_id = :user_id 
                           AND r.eliminado = 0 
                           AND r.fecha_hora >= NOW()
                           AND r.fecha_hora <= DATE_ADD(NOW(), INTERVAL 7 DAY)";
        $stmtReuniones = $db->prepare($queryReuniones);
        $stmtReuniones->execute([':user_id' => $user_id]);
        $stats['reuniones_proximas'] = (int)$stmtReuniones->fetch(PDO::FETCH_ASSOC)['total'];

        // Próximas reuniones
        // Quitada columna 'ubicacion' que no existe en BD
        $queryProximasReuniones = "SELECT r.id, r.titulo, r.fecha_hora,
                                   (SELECT nombre FROM proyectos WHERE id = r.proyecto_id) as proyecto_nombre
                                   FROM reuniones r
                                   INNER JOIN reuniones_usuarios ru ON r.id = ru.reunion_id
                                   WHERE ru.user_id = :user_id 
                                   AND r.eliminado = 0 
                                   AND r.fecha_hora >= NOW()
                                   ORDER BY r.fecha_hora ASC
                                   LIMIT 3";
        $stmtProximasReuniones = $db->prepare($queryProximasReuniones);
        $stmtProximasReuniones->execute([':user_id' => $user_id]);
        $stats['proximas_reuniones'] = $stmtProximasReuniones->fetchAll(PDO::FETCH_ASSOC);

        // Documentos de sus proyectos
        $queryDocs = "SELECT COUNT(DISTINCT d.id) as total 
                      FROM documentos d
                      INNER JOIN proyectos_usuarios pu ON d.proyecto_id = pu.proyecto_id
                      WHERE pu.user_id = :user_id 
                      AND d.eliminado = 0 
                      AND pu.eliminado = 0";
        $stmtDocs = $db->prepare($queryDocs);
        $stmtDocs->execute([':user_id' => $user_id]);
        $stats['total_documentos'] = (int)$stmtDocs->fetch(PDO::FETCH_ASSOC)['total'];

    } elseif ($rol === 'cliente') {
        // ========== CLIENTE: SOLO SUS PROYECTOS ==========
        
        // Proyectos como cliente
        $queryProyectos = "SELECT COUNT(*) as total 
                           FROM proyectos 
                           WHERE cliente_id = :user_id 
                           AND eliminado = 0";
        $stmtProyectos = $db->prepare($queryProyectos);
        $stmtProyectos->execute([':user_id' => $user_id]);
        $stats['total_proyectos'] = (int)$stmtProyectos->fetch(PDO::FETCH_ASSOC)['total'];

        // Proyectos por estado
        $queryEstados = "SELECT estado, COUNT(*) as cantidad 
                         FROM proyectos 
                         WHERE cliente_id = :user_id 
                         AND eliminado = 0 
                         GROUP BY estado";
        $stmtEstados = $db->prepare($queryEstados);
        $stmtEstados->execute([':user_id' => $user_id]);
        $stats['proyectos_por_estado'] = $stmtEstados->fetchAll(PDO::FETCH_ASSOC);

        // Lista de proyectos
        $queryProyectosAsignados = "SELECT id, nombre, estado, 
                                    0 as progreso,
                                    fecha_inicio, fecha_fin
                                    FROM proyectos 
                                    WHERE cliente_id = :user_id 
                                    AND eliminado = 0
                                    ORDER BY fecha_inicio DESC
                                    LIMIT 5";
        $stmtProyectosAsignados = $db->prepare($queryProyectosAsignados);
        $stmtProyectosAsignados->execute([':user_id' => $user_id]);
        $stats['proyectos_asignados'] = $stmtProyectosAsignados->fetchAll(PDO::FETCH_ASSOC);

        // Reuniones próximas
        $queryReuniones = "SELECT COUNT(DISTINCT r.id) as total 
                           FROM reuniones r
                           INNER JOIN proyectos p ON r.proyecto_id = p.id
                           WHERE p.cliente_id = :user_id 
                           AND r.eliminado = 0 
                           AND p.eliminado = 0
                           AND r.fecha_hora >= NOW()
                           AND r.fecha_hora <= DATE_ADD(NOW(), INTERVAL 7 DAY)";
        $stmtReuniones = $db->prepare($queryReuniones);
        $stmtReuniones->execute([':user_id' => $user_id]);
        $stats['reuniones_proximas'] = (int)$stmtReuniones->fetch(PDO::FETCH_ASSOC)['total'];

        // Próximas reuniones
        // Quitada columna 'ubicacion'
        $queryProximasReuniones = "SELECT r.id, r.titulo, r.fecha_hora,
                                   (SELECT nombre FROM proyectos WHERE id = r.proyecto_id) as proyecto_nombre
                                   FROM reuniones r
                                   INNER JOIN proyectos p ON r.proyecto_id = p.id
                                   WHERE p.cliente_id = :user_id 
                                   AND r.eliminado = 0 
                                   AND r.fecha_hora >= NOW()
                                   ORDER BY r.fecha_hora ASC
                                   LIMIT 3";
        $stmtProximasReuniones = $db->prepare($queryProximasReuniones);
        $stmtProximasReuniones->execute([':user_id' => $user_id]);
        $stats['proximas_reuniones'] = $stmtProximasReuniones->fetchAll(PDO::FETCH_ASSOC);

        // Documentos disponibles
        $queryDocs = "SELECT COUNT(*) as total 
                      FROM documentos d
                      INNER JOIN proyectos p ON d.proyecto_id = p.id
                      WHERE p.cliente_id = :user_id 
                      AND d.eliminado = 0 
                      AND p.eliminado = 0";
        $stmtDocs = $db->prepare($queryDocs);
        $stmtDocs->execute([':user_id' => $user_id]);
        $stats['total_documentos'] = (int)$stmtDocs->fetch(PDO::FETCH_ASSOC)['total'];
    }

    // Notificaciones no leídas (para todos los roles)
    $queryNotif = "SELECT COUNT(*) as total 
                   FROM notificaciones 
                   WHERE user_id = :user_id 
                   AND leida = 0 
                   AND eliminado = 0";
    $stmtNotif = $db->prepare($queryNotif);
    $stmtNotif->execute([':user_id' => $user_id]);
    $stats['notificaciones_no_leidas'] = (int)$stmtNotif->fetch(PDO::FETCH_ASSOC)['total'];

    echo json_encode([
        "success" => true,
        "data" => $stats
    ], JSON_UNESCAPED_UNICODE);

} catch (Exception $e) {
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al obtener estadísticas: " . $e->getMessage()
    ]);
}
?>