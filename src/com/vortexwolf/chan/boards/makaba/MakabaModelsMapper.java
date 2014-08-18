package com.vortexwolf.chan.boards.makaba;

import com.vortexwolf.chan.boards.makaba.models.MakabaPostInfo;
import com.vortexwolf.chan.boards.makaba.models.MakabaThreadInfo;
import com.vortexwolf.chan.boards.makaba.models.MakabaThreadsList;
import com.vortexwolf.chan.common.library.MyHtml;
import com.vortexwolf.chan.common.utils.StringUtils;
import com.vortexwolf.chan.common.utils.ThreadPostUtils;
import com.vortexwolf.chan.models.domain.PostModel;
import com.vortexwolf.chan.models.domain.ThreadModel;

public class MakabaModelsMapper {

    public ThreadModel[] mapThreadModels(MakabaThreadsList source){
        ThreadModel[] result = new ThreadModel[source.threads.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.mapThreadModel(source.threads[i]);
        }
        
        return result;
    }
    
    public ThreadModel mapThreadModel(MakabaThreadInfo source){
        ThreadModel model = new ThreadModel();
        model.setReplyCount(source.postsCount);
        model.setImageCount(source.filesCount);
        model.setPosts(this.mapPostModels(source.posts));
        
        return model;
    }
    
    public PostModel[] mapPostModels(MakabaPostInfo[] source){
        PostModel[] result = new PostModel[source.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.mapPostModel(source[i]);
        }
        
        return result;
    }
    
    public PostModel mapPostModel(MakabaPostInfo source){
        PostModel model = new PostModel();
        model.setNumber(source.num);
        model.setName(source.name);
        model.setSubject(MyHtml.fromHtml(StringUtils.emptyIfNull(source.subject)).toString());
        model.setComment(source.comment);
        if (source.files != null && source.files.length > 0) {
            model.setThumbnailUrl(source.files[0].thumbnail);
            model.setImageUrl(source.files[0].path);
            model.setImageSize(source.files[0].size);
            model.setImageWidth(source.files[0].width);
            model.setImageHeight(source.files[0].height);
        }
        model.setTimestamp(source.timestamp != 0 ? source.timestamp * 1000 : ThreadPostUtils.parseMoscowTextDate(source.date));
        model.setParentThread(source.parent);
        
        return model;
    }
}