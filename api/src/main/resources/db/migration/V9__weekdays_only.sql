-- Only Monday to Friday is planned: weekends are left out of every week, including existing ones.
-- Attendees go with their meal (ON DELETE CASCADE).

DELETE FROM meal WHERE EXTRACT(ISODOW FROM meal_date) IN (6, 7);
