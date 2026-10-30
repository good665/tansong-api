package kr.tansong.demo.user.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import kr.tansong.demo.user.domain.User;

@Mapper
public interface UserMapper {

    @Insert("""
        INSERT INTO users (name, email, phone, password_hash, role)
        VALUES (#{name}, #{email}, #{phone}, #{passwordHash}, #{role})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("""
        SELECT id, name, email, phone, password_hash AS passwordHash, role,
               failed_login_attempts AS failedLoginAttempts, locked_until AS lockedUntil
          FROM users WHERE email = #{email}
        """)
    User findByEmail(@Param("email") String email);

    @Select("""
        SELECT id, name, email, phone, password_hash AS passwordHash, role,
               failed_login_attempts AS failedLoginAttempts, locked_until AS lockedUntil
          FROM users WHERE id = #{id}
        """)
    User findById(@Param("id") Long id);

    // 로그인 실패: 실패 횟수를 갱신하고, 임계치를 넘었으면 lockedUntil 을 함께 세팅 (브루트포스 방지)
    @Update("""
        UPDATE users
           SET failed_login_attempts = #{attempts},
               locked_until = #{lockedUntil}
         WHERE id = #{id}
        """)
    int updateLoginFailure(@Param("id") Long id,
                            @Param("attempts") int attempts,
                            @Param("lockedUntil") LocalDateTime lockedUntil);

    // 로그인 성공: 실패 기록 초기화
    @Update("UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = #{id}")
    int resetLoginFailure(@Param("id") Long id);
}
