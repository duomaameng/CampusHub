USE `campus_hub`;

DELETE FROM `task_image`
WHERE `task_id` IN (2, 4);

INSERT INTO `task_image` (`task_id`, `image_url`, `sort_order`)
VALUES (2, '/uploads/image_editor_1781931903418..jpg', 1);
