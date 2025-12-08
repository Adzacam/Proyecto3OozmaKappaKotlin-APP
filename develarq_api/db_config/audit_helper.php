<?php
// db_config/audit_helper.php

/**
 * 🎯 FUNCIÓN PRINCIPAL - Registra auditoría completa con info del dispositivo
 * 
 * @param PDO $db - Conexión a la base de datos
 * @param int $user_id - ID del usuario que realiza la acción
 * @param string $accion - Descripción breve de la acción (ej: "Eliminó el usuario 'Juan Pérez'")
 * @param string $tabla_afectada - Tabla afectada (users, documentos, reuniones, etc.)
 * @param int|null $id_registro_afectado - ID del registro afectado
 * @param array $device_info - Array con información del dispositivo desde el request
 * @param string|null $motivo - Motivo de la acción (ej: justificación de eliminación)
 * @return bool - true si se registró correctamente
 */
function registrarAuditoriaCompleta(
    $db, 
    $user_id, 
    $accion, 
    $tabla_afectada, 
    $id_registro_afectado = null,
    $device_info = [],
    $motivo = null
) {
    try {
        // 1. Capturar información del dispositivo
        $info_dispositivo = [
            'modelo' => $device_info['device_model'] ?? 'No disponible',
            'android_version' => $device_info['android_version'] ?? 'No disponible',
            'sdk_version' => $device_info['sdk_version'] ?? 'No disponible',
            'ip_address' => obtenerIPCliente(),
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? 'No disponible'
        ];

        // 2. Construir descripción detallada
        $descripcion_detallada = construirDescripcionDetallada(
            $accion, 
            $tabla_afectada, 
            $id_registro_afectado,
            $info_dispositivo,
            $motivo
        );

        // 3. Insertar en la base de datos
        $query = "INSERT INTO auditoria_logs 
                  (user_id, accion, descripcion_detallada, tabla_afectada, 
                   id_registro_afectado, ip_address, fecha_accion, created_at) 
                  VALUES 
                  (:user_id, :accion, :descripcion_detallada, :tabla_afectada, 
                   :id_registro_afectado, :ip_address, NOW(), NOW())";
        
        $stmt = $db->prepare($query);
        $stmt->execute([
            ':user_id' => $user_id,
            ':accion' => $accion,
            ':descripcion_detallada' => $descripcion_detallada,
            ':tabla_afectada' => $tabla_afectada,
            ':id_registro_afectado' => $id_registro_afectado,
            ':ip_address' => $info_dispositivo['ip_address']
        ]);
        
        return true;
    } catch (Exception $e) {
        error_log("❌ Error al registrar auditoría: " . $e->getMessage());
        return false;
    }
}

/**
 * 📝 Construye la descripción detallada con formato consistente
 */
function construirDescripcionDetallada($accion, $tabla, $id_registro, $device_info, $motivo = null) {
    $descripcion = "ACCIÓN REALIZADA:\n";
    $descripcion .= "$accion\n\n";
    
    $descripcion .= "DETALLES:\n";
    $descripcion .= "• Tabla: " . getNombreTabla($tabla) . "\n";
    if ($id_registro) {
        $descripcion .= "• ID Registro: $id_registro\n";
    }
    
    // Si hay motivo (ej: eliminación de usuario)
    if ($motivo) {
        $descripcion .= "\nJUSTIFICACIÓN:\n";
        $descripcion .= "$motivo\n";
    }
    
    $descripcion .= "\nINFORMACIÓN DEL DISPOSITIVO:\n";
    $descripcion .= "• Modelo: {$device_info['modelo']}\n";
    $descripcion .= "• Android: {$device_info['android_version']}\n";
    $descripcion .= "• SDK: {$device_info['sdk_version']}\n";
    $descripcion .= "• IP: {$device_info['ip_address']}\n";
    $descripcion .= "• User Agent: {$device_info['user_agent']}\n";
    $descripcion .= "\nFECHA: " . date('d/m/Y H:i:s');
    
    return $descripcion;
}

/**
 * 🏷️ Obtiene nombre legible de la tabla
 */
function getNombreTabla($tabla) {
    $nombres = [
        'users' => 'Usuarios',
        'proyectos' => 'Proyectos',
        'documentos' => 'Documentos',
        'reuniones' => 'Reuniones',
        'tareas' => 'Tareas',
        'notificaciones' => 'Notificaciones'
    ];
    
    return $nombres[strtolower($tabla)] ?? ucfirst($tabla);
}

/**
 * 🌐 Obtiene la IP del cliente
 */
function obtenerIPCliente() {
    $ip = 'DESCONOCIDA';
    
    if (!empty($_SERVER['HTTP_CLIENT_IP'])) {
        $ip = $_SERVER['HTTP_CLIENT_IP'];
    } elseif (!empty($_SERVER['HTTP_X_FORWARDED_FOR'])) {
        $ip = $_SERVER['HTTP_X_FORWARDED_FOR'];
    } elseif (!empty($_SERVER['REMOTE_ADDR'])) {
        $ip = $_SERVER['REMOTE_ADDR'];
    }
    
    return $ip;
}

/**
 * 👤 Obtiene usuario desde token
 */
function obtenerUsuarioDesdeToken($db, $token) {
    try {
        $query = "SELECT id, CONCAT(name, ' ', apellido) as nombre_completo, rol
                  FROM users 
                  WHERE remember_token = :token AND eliminado = 0 
                  LIMIT 1";
        $stmt = $db->prepare($query);
        $stmt->execute([':token' => $token]);
        
        if ($stmt->rowCount() > 0) {
            return $stmt->fetch(PDO::FETCH_ASSOC);
        }
    } catch (Exception $e) {
        error_log("Error al obtener usuario: " . $e->getMessage());
    }
    
    return null;
}

/**
 * 📦 Extrae info del dispositivo desde el request JSON
 */
function extraerInfoDispositivo($data) {
    // Convertir stdClass a array si es necesario
    if (is_object($data)) {
        $data = json_decode(json_encode($data), true);
    }
    
    return [
        'device_model' => isset($data['device_model']) ? $data['device_model'] : 'No disponible',
        'android_version' => isset($data['android_version']) ? $data['android_version'] : 'No disponible',
        'sdk_version' => isset($data['sdk_version']) ? $data['sdk_version'] : 'No disponible'
    ];
}

/**
 * ⚠️ FUNCIÓN LEGACY - Mantener por compatibilidad pero marcar como deprecated
 * @deprecated Usar registrarAuditoriaCompleta() en su lugar
 */
function registrarAuditoria($db, $user_id, $accion, $tabla_afectada, $id_registro_afectado = null, $ip_address = null) {
    return registrarAuditoriaCompleta($db, $user_id, $accion, $tabla_afectada, $id_registro_afectado);
}
?>