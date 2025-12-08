<?php
// ============================================
// ACTUALIZAR PERMISOS DEL PROYECTO
// ============================================
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, PUT");

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
// VALIDAR PERMISOS
// ============================================
$rol_permitido = in_array(strtolower($usuario['rol']), ['admin', 'arquitecto']);
if (!$rol_permitido) {
    http_response_code(403);
    echo json_encode(["success" => false, "message" => "No tienes permisos para modificar permisos del proyecto"]);
    exit();
}

// ============================================
// RECIBIR DATOS
// ============================================
$data = json_decode(file_get_contents("php://input"));

$proyecto_id = isset($data->proyecto_id) ? intval($data->proyecto_id) : null;
$permisos = isset($data->permisos) ? $data->permisos : null;

$device_info = extraerInfoDispositivo($data);

// ============================================
// VALIDACIONES
// ============================================
if (!$proyecto_id || !$permisos || !is_array($permisos)) {
    http_response_code(400);
    echo json_encode(["success" => false, "message" => "Datos inválidos"]);
    exit();
}

// Obtener nombre del proyecto
$query_proyecto = "SELECT nombre FROM proyectos WHERE id = :proyecto_id AND eliminado = 0";
$stmt_proyecto = $db->prepare($query_proyecto);
$stmt_proyecto->execute([':proyecto_id' => $proyecto_id]);

if ($stmt_proyecto->rowCount() == 0) {
    http_response_code(404);
    echo json_encode(["success" => false, "message" => "Proyecto no encontrado"]);
    exit();
}

$proyecto = $stmt_proyecto->fetch(PDO::FETCH_ASSOC);

// ============================================
// ACTUALIZAR PERMISOS
// ============================================
try {
    $db->beginTransaction();

    foreach ($permisos as $permiso_item) {
        $user_id = intval($permiso_item->user_id);
        $permiso_tipo = trim($permiso_item->permiso);

        // Verificar si ya existe la relación
        $query_check = "SELECT id, permiso FROM proyectos_usuarios 
                        WHERE proyecto_id = :proyecto_id AND user_id = :user_id";
        $stmt_check = $db->prepare($query_check);
        $stmt_check->execute([
            ':proyecto_id' => $proyecto_id,
            ':user_id' => $user_id
        ]);

        if ($stmt_check->rowCount() > 0) {
            // Actualizar permiso existente
            $row = $stmt_check->fetch(PDO::FETCH_ASSOC);
            
            if ($row['permiso'] != $permiso_tipo) {
                $query_update = "UPDATE proyectos_usuarios 
                                SET permiso = :permiso, asignado_en = NOW()
                                WHERE proyecto_id = :proyecto_id AND user_id = :user_id";
                $stmt_update = $db->prepare($query_update);
                $stmt_update->execute([
                    ':permiso' => $permiso_tipo,
                    ':proyecto_id' => $proyecto_id,
                    ':user_id' => $user_id
                ]);

                // Registrar en historial de permisos
                $query_historial = "INSERT INTO historial_permisos 
                                   (proyecto_id, usuario_modificador_id, usuario_afectado_id, permiso_asignado, fecha_cambio, created_at, updated_at)
                                   VALUES 
                                   (:proyecto_id, :modificador_id, :afectado_id, :permiso, NOW(), NOW(), NOW())";
                $stmt_historial = $db->prepare($query_historial);
                $stmt_historial->execute([
                    ':proyecto_id' => $proyecto_id,
                    ':modificador_id' => $usuario['id'],
                    ':afectado_id' => $user_id,
                    ':permiso' => $permiso_tipo
                ]);

                // Auditoría
                registrarAuditoriaCompleta(
                    $db,
                    $usuario['id'],
                    "Actualizó el permiso de usuario #$user_id a '$permiso_tipo' en el proyecto '{$proyecto['nombre']}'.",
                    'proyectos_usuarios',
                    $proyecto_id,
                    $device_info
                );

                // Notificar al usuario
                if ($permiso_tipo == 'editar') {
                    $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at, updated_at)
                                    VALUES (:user_id, :mensaje, 'proyecto', 'Permiso actualizado', :url, NOW(), NOW())";
                    $stmt_notif = $db->prepare($query_notif);
                    $stmt_notif->execute([
                        ':user_id' => $user_id,
                        ':mensaje' => "Se te ha asignado permiso '$permiso_tipo' en el proyecto '{$proyecto['nombre']}'.",
                        ':url' => "http://127.0.0.1:8000/proyectos/$proyecto_id"
                    ]);
                }
            }
        } else {
            // Crear nueva asignación si el permiso no es 'ninguno'
            if ($permiso_tipo != 'ninguno') {
                $query_insert = "INSERT INTO proyectos_usuarios (proyecto_id, user_id, permiso, asignado_en)
                                VALUES (:proyecto_id, :user_id, :permiso, NOW())";
                $stmt_insert = $db->prepare($query_insert);
                $stmt_insert->execute([
                    ':proyecto_id' => $proyecto_id,
                    ':user_id' => $user_id,
                    ':permiso' => $permiso_tipo
                ]);

                // Registrar en historial
                $query_historial = "INSERT INTO historial_permisos 
                                   (proyecto_id, usuario_modificador_id, usuario_afectado_id, permiso_asignado, fecha_cambio, created_at, updated_at)
                                   VALUES 
                                   (:proyecto_id, :modificador_id, :afectado_id, :permiso, NOW(), NOW(), NOW())";
                $stmt_historial = $db->prepare($query_historial);
                $stmt_historial->execute([
                    ':proyecto_id' => $proyecto_id,
                    ':modificador_id' => $usuario['id'],
                    ':afectado_id' => $user_id,
                    ':permiso' => $permiso_tipo
                ]);

                // Notificar
                $query_notif = "INSERT INTO notificaciones (user_id, mensaje, tipo, asunto, url, created_at, updated_at)
                                VALUES (:user_id, :mensaje, 'proyecto', 'Permiso actualizado', :url, NOW(), NOW())";
                $stmt_notif = $db->prepare($query_notif);
                $stmt_notif->execute([
                    ':user_id' => $user_id,
                    ':mensaje' => "Se te ha asignado permiso '$permiso_tipo' en el proyecto '{$proyecto['nombre']}'.",
                    ':url' => "http://127.0.0.1:8000/proyectos/$proyecto_id"
                ]);
            }
        }
    }

    // Auditoría final
    registrarAuditoriaCompleta(
        $db,
        $usuario['id'],
        "Finalizó actualización de permisos del proyecto '{$proyecto['nombre']}'.",
        'proyectos_usuarios',
        $proyecto_id,
        $device_info
    );

    $db->commit();

    echo json_encode([
        "success" => true,
        "message" => "Permisos actualizados exitosamente"
    ]);
    
} catch (Exception $e) {
    $db->rollBack();
    http_response_code(500);
    echo json_encode([
        "success" => false,
        "message" => "Error al actualizar permisos: " . $e->getMessage()
    ]);
}
?>