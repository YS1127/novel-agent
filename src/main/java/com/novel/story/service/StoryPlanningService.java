package com.novel.story.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.novel.ai.AiChatClient;
import com.novel.ai.AiContentParser;
import com.novel.ai.AiContentParser.Plan;
import com.novel.story.StoryConflictException;
import com.novel.story.entity.Story;
import com.novel.story.entity.StoryQuestion;
import com.novel.story.mapper.StoryMapper;
import com.novel.story.mapper.StoryQuestionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class StoryPlanningService {
    private final StoryService stories;
    private final StoryMapper storyMapper;
    private final StoryQuestionMapper questions;
    private final AiChatClient ai;
    private final AiContentParser parser;
    private final TransactionTemplate transactions;

    public record Answer(long id, String answer) {}

    public StoryPlanningService(StoryService stories, StoryMapper storyMapper, StoryQuestionMapper questions,
            AiChatClient ai, AiContentParser parser, TransactionTemplate transactions) {
        this.stories = stories;
        this.storyMapper = storyMapper;
        this.questions = questions;
        this.ai = ai;
        this.parser = parser;
        this.transactions = transactions;
    }

    public List<StoryQuestion> listQuestions(long storyId) {
        stories.get(storyId);
        return questions.selectList(new LambdaQueryWrapper<StoryQuestion>()
                .eq(StoryQuestion::getStoryId, storyId).orderByAsc(StoryQuestion::getOrdinal));
    }

    public List<StoryQuestion> generateQuestions(long storyId, int version) {
        Story story = stories.get(storyId);
        ensureVersion(story, version);
        if ("CONFIRMED".equals(story.getStage()) || "DRAFT".equals(story.getStage())) {
            throw new StoryConflictException("已确认的大纲不能重新生成问题");
        }
        String response = ai.complete("你是中文小说构思助手。只返回 JSON：{\"questions\":[\"问题1\",\"问题2\"]}。最多三个直接影响故事走向的关键问题，不要解释。",
                "故事点子：" + story.getIdea() + "\n文风说明：" + story.getStyleGuide());
        List<String> generated = parser.questions(response);
        return transactions.execute(status -> {
            updateStage(storyId, version, "QUESTIONS", null, null);
            questions.delete(new LambdaQueryWrapper<StoryQuestion>().eq(StoryQuestion::getStoryId, storyId));
            int ordinal = 0;
            for (String text : generated) {
                StoryQuestion question = new StoryQuestion();
                question.setStoryId(storyId);
                question.setOrdinal(++ordinal);
                question.setQuestion(text);
                question.setAnswer("");
                question.setCreatedAt(LocalDateTime.now());
                questions.insert(question);
            }
            return listQuestions(storyId);
        });
    }

    public List<StoryQuestion> saveAnswers(long storyId, int version, List<Answer> answers) {
        if (answers == null) throw new IllegalArgumentException("回答不能为空");
        return transactions.execute(status -> {
            Story story = stories.get(storyId);
            ensureVersion(story, version);
            List<StoryQuestion> existing = listQuestions(storyId);
            Map<Long, StoryQuestion> byId = existing.stream().collect(Collectors.toMap(StoryQuestion::getId, q -> q));
            for (Answer answer : answers) {
                StoryQuestion target = byId.get(answer.id());
                if (target == null) throw new IllegalArgumentException("问题不属于当前作品");
                target.setAnswer(answer.answer() == null ? "" : answer.answer().trim());
                questions.updateById(target);
            }
            updateStage(storyId, version, "QUESTIONS", null, null);
            return listQuestions(storyId);
        });
    }

    public Story generatePlan(long storyId, int version) {
        Story story = stories.get(storyId);
        ensureVersion(story, version);
        if ("CONFIRMED".equals(story.getStage()) || "DRAFT".equals(story.getStage())) {
            throw new StoryConflictException("已确认的大纲不能重新生成方案");
        }
        String answers = listQuestions(storyId).stream()
                .map(q -> q.getQuestion() + "\n回答：" + (q.getAnswer() == null ? "" : q.getAnswer()))
                .collect(Collectors.joining("\n\n"));
        String response = ai.complete("你是中文小说策划编辑。只返回 JSON 对象，字段 proposal 为完整故事方案，outline 为按情节顺序分段的大纲。不要解释。",
                "标题：" + story.getTitle() + "\n点子：" + story.getIdea() + "\n文风：" + story.getStyleGuide() + "\n问答：" + answers);
        Plan plan = parser.plan(response);
        return transactions.execute(status -> {
            updateStage(storyId, version, "PLAN", plan.proposal(), plan.outline());
            return stories.get(storyId);
        });
    }

    public Story confirmPlan(long storyId, int version) {
        return transactions.execute(status -> {
            Story story = stories.get(storyId);
            ensureVersion(story, version);
            if (story.getProposal() == null || story.getProposal().isBlank()
                    || story.getOutline() == null || story.getOutline().isBlank()) {
                throw new IllegalArgumentException("请先完善故事方案和大纲");
            }
            updateStage(storyId, version, "CONFIRMED", null, null);
            return stories.get(storyId);
        });
    }

    private void updateStage(long storyId, int version, String stage, String proposal, String outline) {
        LambdaUpdateWrapper<Story> update = new LambdaUpdateWrapper<Story>()
                .eq(Story::getId, storyId).eq(Story::getVersion, version)
                .set(Story::getStage, stage)
                .set(Story::getVersion, version + 1)
                .set(Story::getUpdatedAt, LocalDateTime.now());
        if (proposal != null) update.set(Story::getProposal, proposal);
        if (outline != null) update.set(Story::getOutline, outline);
        if (storyMapper.update(null, update) != 1) throw new StoryConflictException("作品已被修改，请刷新后重试");
    }

    private static void ensureVersion(Story story, int version) {
        if (story.getVersion() != version) throw new StoryConflictException("作品已被修改，请刷新后重试");
    }
}
