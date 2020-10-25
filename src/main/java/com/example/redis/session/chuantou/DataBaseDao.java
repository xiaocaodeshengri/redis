package com.example.redis.session.chuantou;

import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

@Repository
public interface DataBaseDao {
    String get(String key);
}
