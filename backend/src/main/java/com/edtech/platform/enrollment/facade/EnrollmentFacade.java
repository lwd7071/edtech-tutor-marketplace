package com.edtech.platform.enrollment.facade;

import java.util.UUID;

public interface EnrollmentFacade {
    
    /**
     * Checks if a valid learning relationship exists between a teacher and a student.
     *
     * @param teacherId the UUID of the teacher
     * @param studentId the UUID of the student
     * @return true if an ACTIVE or COMPLETED student package exists, false otherwise
     */
    boolean hasValidRelationship(UUID teacherId, UUID studentId);

    /**
     * Checks if a student package exists for a given pricing package.
     *
     * @param pricingPackageId the UUID of the pricing package
     * @return true if a student package exists, false otherwise
     */
    boolean hasStudentPackage(UUID pricingPackageId);
}
