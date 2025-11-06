package project.matchalatte.support.scheduling;

import org.springframework.stereotype.Service;

@Service
public class SchedulingService {

    private final SchedulingMapper schedulingMapper;

    public SchedulingService(SchedulingMapper schedulingMapper) {
        this.schedulingMapper = schedulingMapper;
    }

    public void schedulingMapper(String name, String description, Long price) {
        schedulingMapper.csvMapper(name, description, price);
    }

}
