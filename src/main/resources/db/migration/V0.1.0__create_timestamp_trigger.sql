CREATE OR REPLACE FUNCTION update_endret_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.endret_timestamp = now();
    RETURN NEW;
END;
$$ language 'plpgsql';