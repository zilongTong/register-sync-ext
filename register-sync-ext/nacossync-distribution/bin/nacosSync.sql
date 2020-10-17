/******************************************/
/*   DB name = nacos_Sync   */
/*   Table name = cluster   */
/******************************************/
CREATE TABLE `cluster` (
  `id` int(11) NOT NULL AUTO_INCREMENT comment 'id',
  `cluster_id` varchar(255) not null DEFAULT '' comment 'cluster_id',
  `cluster_name` varchar(255) not null DEFAULT '' comment 'cluster_name',
  `cluster_type` varchar(255)not null DEFAULT '' comment 'cluster_type',
  `connect_key_list` varchar(255) not null DEFAULT '' comment 'connect_key_list',
  PRIMARY KEY (`id`)
) ENGINE=innodb AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4  comment 'cluster';
/******************************************/
/*   DB name = nacos_Sync   */
/*   Table name = system_config   */
/******************************************/
CREATE TABLE `system_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT comment 'id',
  `config_desc` varchar(255) not null DEFAULT '' comment 'config_desc',
  `config_key` varchar(255) not null DEFAULT ''  comment 'config_key',
  `config_value` varchar(255) not null DEFAULT ''  comment 'config_value',
  PRIMARY KEY (`id`)
) ENGINE=innodb DEFAULT CHARSET=utf8mb4  comment 'system_config';
/******************************************/
/*   DB name = nacos_Sync   */
/*   Table name = task   */
/******************************************/
CREATE TABLE `task` (
  `id` int(11) NOT NULL AUTO_INCREMENT comment 'id',
  `dest_cluster_id` varchar(255) not null DEFAULT ''  comment 'dest_cluster_id',
  `group_name` varchar(255) not null DEFAULT ''  comment 'group_name',
  `name_space` varchar(255) not null DEFAULT ''  comment 'name_space',
  `operation_id` varchar(255) not null DEFAULT '' comment 'operation_id',
  `service_name` varchar(255) not null DEFAULT '' comment 'service_name',
  `source_cluster_id` varchar(255) not null DEFAULT '' comment 'source_cluster_id',
  `task_id` varchar(255) COLLATE not null DEFAULT '' comment 'task_id',
  `task_status` varchar(255) not null DEFAULT '' comment 'task_status',
  `version` varchar(255) not null DEFAULT '' comment 'version',
  `worker_ip` varchar(255) not null DEFAULT '' comment 'worker_ip',
  PRIMARY KEY (`id`)
) ENGINE=innodb AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4  comment 'task';
