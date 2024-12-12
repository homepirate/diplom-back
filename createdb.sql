-- Doctors Table
CREATE TABLE doctors (
    id SERIAL PRIMARY KEY,
    fullname VARCHAR(100) NOT NULL,
    specialization VARCHAR(100),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100) UNIQUE,
	unique_code CHAR(7) UNIQUE NOT NULL
);

-- Patients Table
CREATE TABLE patients (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    birth_date DATE,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100) UNIQUE
);

-- Doctor_Patient Table (Many-to-Many)
CREATE TABLE doctor_patient (
    doctor_id INT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    PRIMARY KEY (doctor_id, patient_id)
);

-- Visits Table
CREATE TABLE visits (
    id SERIAL PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id INT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    visit_date TIMESTAMP NOT NULL,
    notes TEXT
);

-- Services Table
CREATE TABLE services (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL
);

-- Visit_Service Table (Many-to-Many between Visits and Services)
CREATE TABLE visit_service (
    visit_id INT NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    service_id INT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    PRIMARY KEY (visit_id, service_id)
);

-- Attachments Table (One-to-One with Visits)
CREATE TABLE attachments (
    id SERIAL PRIMARY KEY,
    visit_id INT NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    file_path TEXT NOT NULL,
    description TEXT
);
