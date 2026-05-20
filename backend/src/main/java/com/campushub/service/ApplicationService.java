package com.campushub.service;

import com.campushub.dto.response.ApplicationResponse;

import java.util.List;

public interface ApplicationService {

    List<ApplicationResponse> listApplications(Long taskId);

    ApplicationResponse apply(Long taskId, String message);

    void confirm(Long applicationId);

    void reject(Long applicationId);
}
