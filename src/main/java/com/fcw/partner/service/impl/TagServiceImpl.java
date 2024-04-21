package com.fcw.partner.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fcw.partner.model.domain.Tag;
import com.fcw.partner.mapper.TagMapper;
import com.fcw.partner.service.TagService;
import org.springframework.stereotype.Service;

/**
* @author chengwu
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2024-04-20 20:45:08
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{
    
}




