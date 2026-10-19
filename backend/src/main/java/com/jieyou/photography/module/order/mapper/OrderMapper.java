package com.jieyou.photography.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jieyou.photography.module.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 订单数据访问。
 * 列表/详情需要展示用户名、主题名、套餐名，使用联表查询；
 * 所有筛选参数均通过 #{} 参数化，避免 SQL 注入（O-07）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    String ORDER_COLUMNS = """
            o.id, o.order_no, o.user_id, u.username,
            o.theme_id, t.name AS theme_name,
            o.package_id, p.name AS package_name,
            o.shoot_date, o.shoot_slot, o.remark, o.status,
            o.reject_reason, o.price, o.deliver_time,
            o.create_time, o.update_time
            """;

    @Select("""
            <script>
            SELECT
            """ + ORDER_COLUMNS + """
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id
            LEFT JOIN t_theme t ON t.id = o.theme_id
            LEFT JOIN t_package p ON p.id = o.package_id
            <where>
                <if test="userId != null">AND o.user_id = #{userId}</if>
                <if test="status != null">AND o.status = #{status}</if>
                <if test="keyword != null">AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR t.name LIKE CONCAT('%', #{keyword}, '%'))</if>
            </where>
            ORDER BY o.create_time DESC, o.id DESC
            </script>
            """)
    Page<OrderRow> selectOrderPage(Page<OrderRow> page,
                                   @Param("userId") Long userId,
                                   @Param("status") String status,
                                   @Param("keyword") String keyword);

    @Select("""
            SELECT
            """ + ORDER_COLUMNS + """
            FROM t_order o
            LEFT JOIN t_user u ON u.id = o.user_id
            LEFT JOIN t_theme t ON t.id = o.theme_id
            LEFT JOIN t_package p ON p.id = o.package_id
            WHERE o.id = #{id}
            """)
    OrderRow selectOrderRow(@Param("id") Long id);
}
