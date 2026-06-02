USE `campus_hub`;

UPDATE `credit_log`
SET
  `score_before` = LEAST(100, GREATEST(0, `score_before`)),
  `score_after` = LEAST(100, GREATEST(0, `score_after`));

UPDATE `credit_log`
SET `change_amount` = `score_after` - `score_before`;
