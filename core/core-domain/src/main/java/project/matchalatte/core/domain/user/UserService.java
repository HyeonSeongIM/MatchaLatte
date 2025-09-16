package project.matchalatte.core.domain.user;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import project.matchalatte.support.logging.LogData;
import project.matchalatte.support.logging.TraceIdContext;
import project.matchalatte.support.logging.UserIdContext;

@Service
public class UserService {
    private final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserReader userReader;
    private final UserWriter userWriter;

    public UserService(UserReader userReader, UserWriter userWriter) {
        this.userReader = userReader;
        this.userWriter = userWriter;
    }

    public User add(String username, String password, String nickname) {
        String traceId = TraceIdContext.traceId();
        Long userId = UserIdContext.getCurrentUserId();
        log.info("{}", LogData.of(traceId, userId, "유저정보 가져오기", "유저 등록 요청"));
        User result = userWriter.add(username, password, nickname);
        log.info("{}", LogData.of(traceId, userId, "유저정보 가져오기", "유저 등록 완료"));
        return result;
    }

    public User read(Long id){
        return userReader.read(id);
    }
}
